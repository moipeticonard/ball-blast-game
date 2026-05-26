package com.ballblast.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.sqrt

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    
    private var gameThread: Thread? = null
    private var running = false
    
    private val paint = Paint().apply { isAntiAlias = true }
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Balle joueur
    private var ballX = 0f
    private var ballY = 0f
    private val ballRadius = 30f
    private val ballSpeed = 8f
    
    // Projectiles
    private var projectiles = mutableListOf<Projectile>()
    
    // Ennemis
    private var enemies = mutableListOf<Enemy>()
    private var score = 0
    
    init {
        holder.addCallback(this)
        setFocusable(true)
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width
        screenHeight = height
        ballX = screenWidth / 2f
        ballY = screenHeight - 100f
        
        // Spawn initial enemies
        spawnEnemies()
        
        running = true
        gameThread = Thread(this)
        gameThread?.start()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        gameThread?.join()
    }
    
    override fun run() {
        while (running) {
            update()
            draw()
            Thread.sleep(16) // ~60 FPS
        }
    }
    
    private fun update() {
        // Collision avec projectiles
        val projectilesToRemove = mutableListOf<Projectile>()
        val enemiesToRemove = mutableListOf<Enemy>()
        
        for (projectile in projectiles) {
            projectile.update()
            
            for (enemy in enemies) {
                val distance = sqrt(
                    (projectile.x - enemy.x) * (projectile.x - enemy.x) +
                    (projectile.y - enemy.y) * (projectile.y - enemy.y)
                )
                
                if (distance < enemy.radius + projectile.radius) {
                    enemy.health--
                    projectilesToRemove.add(projectile)
                    
                    if (enemy.health <= 0) {
                        enemiesToRemove.add(enemy)
                        score += 10
                    }
                }
            }
            
            if (projectile.y < 0) {
                projectilesToRemove.add(projectile)
            }
        }
        
        projectiles.removeAll(projectilesToRemove)
        enemies.removeAll(enemiesToRemove)
        
        // Spawn nouveaux ennemis
        if (enemies.isEmpty()) {
            spawnEnemies()
        }
    }
    
    private fun draw() {
        val canvas = holder.lockCanvas() ?: return
        
        // Fond noir
        canvas.drawColor(Color.BLACK)
        
        // Balle joueur
        paint.color = Color.CYAN
        canvas.drawCircle(ballX, ballY, ballRadius, paint)
        
        // Projectiles
        paint.color = Color.YELLOW
        for (projectile in projectiles) {
            canvas.drawCircle(projectile.x, projectile.y, projectile.radius, paint)
        }
        
        // Ennemis
        for (enemy in enemies) {
            paint.color = Color.RED
            canvas.drawCircle(enemy.x, enemy.y, enemy.radius, paint)
            
            // Health bar
            paint.color = Color.WHITE
            canvas.drawRect(
                enemy.x - enemy.radius,
                enemy.y - enemy.radius - 15f,
                enemy.x + enemy.radius,
                enemy.y - enemy.radius - 10f,
                paint
            )
            paint.color = Color.GREEN
            canvas.drawRect(
                enemy.x - enemy.radius,
                enemy.y - enemy.radius - 15f,
                enemy.x - enemy.radius + (2 * enemy.radius * enemy.health / 3f),
                enemy.y - enemy.radius - 10f,
                paint
            )
        }
        
        // Score
        paint.color = Color.WHITE
        paint.textSize = 50f
        canvas.drawText("Score: $score", 50f, 80f, paint)
        
        holder.unlockCanvasAndPost(canvas)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                ballX = event.x.coerceIn(ballRadius, screenWidth - ballRadius)
                ballY = event.y.coerceIn(ballRadius, screenHeight - ballRadius)
            }
            MotionEvent.ACTION_DOWN -> {
                // Lancer un projectile
                val projectile = Projectile(ballX, ballY - ballRadius)
                projectiles.add(projectile)
            }
        }
        return true
    }
    
    private fun spawnEnemies() {
        for (i in 0..2) {
            val x = (Math.random() * (screenWidth - 100) + 50).toFloat()
            val y = (Math.random() * (screenHeight / 3) + 50).toFloat()
            enemies.add(Enemy(x, y))
        }
    }
    
    data class Projectile(var x: Float, var y: Float, val radius: Float = 10f, val speed: Float = 15f) {
        fun update() {
            y -= speed
        }
    }
    
    data class Enemy(var x: Float, var y: Float, val radius: Float = 40f, var health: Int = 3)
}