package com.anwesh.uiprojects.halfarcbetweenbarsview

/**
 * Created by anweshmishra on 09/09/20.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.app.Activity
import android.content.Context

val colors : Array<Int> = arrayOf(
        "#F44336",
        "#4CAF50",
        "#FF5722",
        "#2196F3",
        "#3F51B5"
).map({Color.parseColor(it)}).toTypedArray()
val parts : Int = 3
val barSizeFactor : Float = 5.2f
val scGap : Float = 0.02f / parts
val backColor : Int = Color.parseColor("#BDBDBD")
val delay : Long = 20
val start : Float = 180f
val sweep : Float = 180f

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawHalfArcBar(scale : Float, w : Float, h : Float, paint : Paint) {
    val barSize : Float = w / barSizeFactor
    val r : Float = (w - 2 * barSize) / 2
    val sf : Float = scale.sinify()
    val sf1 : Float = sf.divideScale(0, parts)
    val sf2 : Float = sf.divideScale(1, parts)
    save()
    translate(w / 2, h / 2)
    for (j in 0..1) {
        save()
        scale(1f - 2 * j, 1f)
        translate(0f, h / 2)
        drawRect(RectF(w / 2 - barSize, 0f, w / 2, -r * sf1), paint)
        restore()
    }
    drawArc(RectF(-r, h / 2 - r, r, h / 2 + r), start, sweep * sf2, true, paint)
    restore()
}

fun Canvas.drawHABBNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    paint.color = colors[i]
    drawHalfArcBar(scale, w, h, paint)
}

class HalfArcBetweenBarsView(ctx : Context) : View(ctx) {

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += dir * scGap
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class HABBNode(var i : Int, val state : State = State()) {

        private var next : HABBNode? = null
        private var prev : HABBNode? = null

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < colors.size - 1) {
                next = HABBNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawHABBNode(i, state.scale, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : HABBNode {
            var curr : HABBNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class HalfArcBetweenBars(var i : Int) {

        private var curr : HABBNode = HABBNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : HalfArcBetweenBarsView) {

        private val animator : Animator = Animator(view)
        private val habb : HalfArcBetweenBars = HalfArcBetweenBars(0)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            habb.draw(canvas, paint)
            animator.animate {
                habb.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            habb.startUpdating {
                animator.start()
            }
        }
    }
}