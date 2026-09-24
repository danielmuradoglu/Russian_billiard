package com.bmpodium.russianbilliards

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(BilliardsView(this))
    }
}

private data class Ball(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var pocketed: Boolean = false,
    val cue: Boolean = false
)

private class BilliardsView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val balls = mutableListOf<Ball>()

    private var left = 0f
    private var right = 0f
    private var top = 0f
    private var bottom = 0f

    private var radius = 18f
    private var pocketRadius = 28f

    private var initialized = false
    private var aiming = false
    private var moving = false
    private var scoredThisTurn = false

    private var aimX = 0f
    private var aimY = 0f

    private var player = 1
    private var score1 = 0
    private var score2 = 0

    private val cueBall: Ball?
        get() = balls.firstOrNull { it.cue }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)

        left = w * 0.08f
        right = w * 0.92f
        top = h * 0.12f
        bottom = h * 0.88f

        radius = min(w, h) * 0.025f
        pocketRadius = radius * 1.55f

        resetGame()
        initialized = true
    }

    private fun resetGame() {
        balls.clear()

        player = 1
        score1 = 0
        score2 = 0
        moving = false
        aiming = false
        scoredThisTurn = false

        val centerY = (top + bottom) / 2f
        val tableWidth = right - left

        balls.add(
            Ball(
                x = left + tableWidth * 0.25f,
                y = centerY,
                cue = true
            )
        )

        val rackX = left + tableWidth * 0.68f
        val gap = radius * 2.04f

        var row = 0
        var count = 0

        while (row < 5 && count < 15) {
            for (i in 0..row) {
                if (count >= 15) break

                val x = rackX + row * gap * 0.88f
                val y = centerY + (i - row / 2f) * gap

                balls.add(Ball(x, y))
                count++
            }
            row++
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.rgb(32, 24, 18))

        paint.color = Color.rgb(82, 48, 25)
        canvas.drawRoundRect(
            RectF(
                left - 32f,
                top - 32f,
                right + 32f,
                bottom + 32f
            ),
            25f,
            25f,
            paint
        )

        paint.color = Color.rgb(18, 92, 55)
        canvas.drawRect(left, top, right, bottom, paint)

        drawPockets(canvas)
        drawBalls(canvas)
        drawHud(canvas)

        if (!moving && aiming) {
            drawAim(canvas)
        }
    }

    private fun drawPockets(canvas: Canvas) {
        paint.color = Color.BLACK

        val pockets = pocketPositions()

        for ((x, y) in pockets) {
            canvas.drawCircle(x, y, pocketRadius, paint)
        }
    }

    private fun drawBalls(canvas: Canvas) {
        for (ball in balls) {
            if (ball.pocketed) continue

            paint.color = if (ball.cue) {
                Color.rgb(245, 215, 120)
            } else {
                Color.WHITE
            }

            canvas.drawCircle(ball.x, ball.y, radius, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = Color.rgb(180, 180, 180)
            canvas.drawCircle(ball.x, ball.y, radius, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawHud(canvas: Canvas) {
        paint.color = Color.WHITE
        paint.textSize = 30f

        canvas.drawText(
            "Игрок 1: $score1",
            left,
            top - 48f,
            paint
        )

        canvas.drawText(
            "Игрок 2: $score2",
            right - 180f,
            top - 48f,
            paint
        )

        paint.textSize = 26f
        canvas.drawText(
            "Ход игрока $player",
            (left + right) / 2f - 85f,
            top - 48f,
            paint
        )
    }

    private fun drawAim(canvas: Canvas) {
        val cue = cueBall ?: return
        if (cue.pocketed) return

        paint.color = Color.WHITE
        paint.strokeWidth = 3f

        canvas.drawLine(
            cue.x,
            cue.y,
            aimX,
            aimY,
            paint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!initialized || moving) return true

        val cue = cueBall ?: return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                aiming = true
                aimX = event.x
                aimY = event.y
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                aimX = event.x
                aimY = event.y
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                if (!aiming) return true

                aiming = false

                val dx = cue.x - event.x
                val dy = cue.y - event.y

                val distance = sqrt(dx * dx + dy * dy)

                if (distance > 10f) {
                    val power = min(distance * 0.075f, 32f)

                    cue.vx = dx / distance * power
                    cue.vy = dy / distance * power

                    scoredThisTurn = false
                    moving = true
                }

                invalidate()
            }
        }

        return true
    }

    private fun stepPhysics() {
        if (!moving) return

        var anyMoving = false

        for (ball in balls) {
            if (ball.pocketed) continue

            ball.x += ball.vx
            ball.y += ball.vy

            ball.vx *= 0.989f
            ball.vy *= 0.989f

            if (abs(ball.vx) < 0.025f) ball.vx = 0f
            if (abs(ball.vy) < 0.025f) ball.vy = 0f

            cushion(ball)
            checkPocket(ball)

            if (ball.vx != 0f || ball.vy != 0f) {
                anyMoving = true
            }
        }

        for (i in 0 until balls.size) {
            for (j in i + 1 until balls.size) {
                collide(balls[i], balls[j])
            }
        }

        if (!anyMoving) {
            finishTurn()
        }

        invalidate()
    }

    private fun cushion(ball: Ball) {
        if (ball.pocketed) return

        if (ball.x - radius < left) {
            ball.x = left + radius
            ball.vx = abs(ball.vx) * 0.92f
        }

        if (ball.x + radius > right) {
            ball.x = right - radius
            ball.vx = -abs(ball.vx) * 0.92f
        }

        if (ball.y - radius < top) {
            ball.y = top + radius
            ball.vy = abs(ball.vy) * 0.92f
        }

        if (ball.y + radius > bottom) {
            ball.y = bottom - radius
            ball.vy = -abs(ball.vy) * 0.92f
        }
    }

    private fun checkPocket(ball: Ball) {
        if (ball.pocketed) return

        for ((px, py) in pocketPositions()) {
            val dx = ball.x - px
            val dy = ball.y - py

            if (dx * dx + dy * dy < pocketRadius * pocketRadius * 0.72f) {
                ball.pocketed = true
                ball.vx = 0f
                ball.vy = 0f

                if (!ball.cue) {
                    if (player == 1) {
                        score1++
                    } else {
                        score2++
                    }

                    scoredThisTurn = true
                }

                return
            }
        }
    }

    private fun collide(a: Ball, b: Ball) {
        if (a.pocketed || b.pocketed) return

        val dx = b.x - a.x
        val dy = b.y - a.y

        val distanceSquared = dx * dx + dy * dy
        val minimumDistance = radius * 2f

        if (
            distanceSquared <= 0f ||
            distanceSquared >= minimumDistance * minimumDistance
        ) {
            return
        }

        val distance = sqrt(distanceSquared)
        val nx = dx / distance
        val ny = dy / distance

        val overlap = minimumDistance - distance

        a.x -= nx * overlap / 2f
        a.y -= ny * overlap / 2f

        b.x += nx * overlap / 2f
        b.y += ny * overlap / 2f

        val relativeVelocity =
            (b.vx - a.vx) * nx +
            (b.vy - a.vy) * ny

        if (relativeVelocity < 0f) {
            val impulse = -relativeVelocity * 0.97f

            a.vx -= impulse * nx
            a.vy -= impulse * ny

            b.vx += impulse * nx
            b.vy += impulse * ny
        }
    }

    private fun finishTurn() {
        moving = false

        val cue = cueBall

        if (cue != null && cue.pocketed) {
            cue.pocketed = false
            cue.x = left + (right - left) * 0.25f
            cue.y = (top + bottom) / 2f
            cue.vx = 0f
            cue.vy = 0f

            player = if (player == 1) 2 else 1
        } else if (!scoredThisTurn) {
            player = if (player == 1) 2 else 1
        }

        if (score1 >= 8 || score2 >= 8) {
            val winner = if (score1 >= 8) 1 else 2

            Toast.makeText(
                context,
                "Игрок $winner победил! Новая партия.",
                Toast.LENGTH_LONG
            ).show()

            resetGame()
        }

        scoredThisTurn = false
    }

    private fun pocketPositions(): List<Pair<Float, Float>> {
        return listOf(
            Pair(left, top),
            Pair((left + right) / 2f, top),
            Pair(right, top),
            Pair(left, bottom),
            Pair((left + right) / 2f, bottom),
            Pair(right, bottom)
        )
    }

    private val loop = object : Runnable {
        override fun run() {
            stepPhysics()
            postDelayed(this, 16L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(loop)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(loop)
        super.onDetachedFromWindow()
    }
}
