package com.bmpodium.russianbilliards

import android.app.*
import android.os.Bundle
import android.graphics.*
import android.view.*
import android.content.*
import kotlin.math.*
import java.util.Random

data class Ball(var x:Float,var y:Float,var vx:Float=0f,var vy:Float=0f,var pocketed:Boolean=false,val cue:Boolean=false)

class MainActivity: Activity(){
 override fun onCreate(b:Bundle?){super.onCreate(b); window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); decorFullscreen(); setContentView(GameView(this))}
 private fun decorFullscreen(){ window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY }
}

class GameView(c:Context):View(c){
 private val p=Paint(Paint.ANTI_ALIAS_FLAG); private val balls=mutableListOf<Ball>(); private var cue:Ball?=null
 private var left=0f;private var top=0f;private var right=0f;private var bottom=0f;private var r=18f;private var pocket=23f
 private var aimX=0f;private var aimY=0f;private var dragging=false; private var power=0f; private var player=1; private var s1=0;private var s2=0;private var moving=false;private var scored=false
 private val rnd=Random()
 init { p.typeface=Typeface.create("sans",Typeface.BOLD) }
 override fun onSizeChanged(w:Int,h:Int,ow:Int,oh:Int){ left=w*.075f;right=w*.925f;top=h*.11f;bottom=h*.89f;r=min((bottom-top)/20f,(right-left)/40f);pocket=r*1.34f;reset() }
 private fun reset(){ balls.clear();s1=0;s2=0;player=1; setupRack();invalidate() }
 private fun setupRack(){
  cue=Ball(left+(right-left)*.25f,(top+bottom)/2,cue=true);balls.add(cue!!)
  val sx=left+(right-left)*.70f;val sy=(top+bottom)/2; var row=0
  for(i in 0 until 15){ while(i>=((row+1)*(row+2))/2) row++; val first=row*(row+1)/2;val k=i-first; val x=sx+row*r*1.76f;val y=sy+(k-row/2f)*r*2.02f;balls.add(Ball(x,y)) }
  aimX=right;aimY=sy
 }
 override fun onDraw(c:Canvas){super.onDraw(c); c.drawColor(Color.rgb(28,19,11));
  p.color=Color.rgb(105,67,31);c.drawRoundRect(left-r*2.5f,top-r*2.5f,right+r*2.5f,bottom+r*2.5f,r*2,r*2,p)
  p.color=Color.rgb(19,92,60);c.drawRect(left,top,right,bottom,p)
  drawPockets(c);drawHud(c); for(b in balls) if(!b.pocketed) drawBall(c,b)
  val q=cue; if(q!=null&&!q.pocketed&&!moving){ val dx=aimX-q.x;val dy=aimY-q.y;val d=max(1f,sqrt(dx*dx+dy*dy));val ux=dx/d;val uy=dy/d
   p.strokeWidth=2f;p.color=Color.argb(180,255,255,255);c.drawLine(q.x,q.y,q.x+ux*min(d,800f),q.y+uy*min(d,800f),p)
   p.strokeWidth=r*.42f;p.color=Color.rgb(202,167,103);val back=65f+power*1.2f;c.drawLine(q.x-ux*(r+back),q.y-uy*(r+back),q.x-ux*(r+back+220),q.y-uy*(r+back+220),p)
  }
  postInvalidateOnAnimation()
 }
 private fun drawPockets(c:Canvas){p.color=Color.BLACK; val pts=arrayOf(left to top,(left+right)/2 to top,right to top,left to bottom,(left+right)/2 to bottom,right to bottom);for(pt in pts)c.drawCircle(pt.first,pt.second,pocket,p)}
 private fun drawHud(c:Canvas){p.textSize=24f;p.color=Color.WHITE;p.textAlign=Paint.Align.CENTER;c.drawText("ИГРОК 1: $s1     •     ИГРОК 2: $s2",width/2f,34f,p);p.textSize=18f;c.drawText("Ход игрока $player   |   Потяни от битка назад/в сторону и отпусти для удара",width/2f,height-12f,p)}
 private fun drawBall(c:Canvas,b:Ball){p.color=Color.argb(80,0,0,0);c.drawCircle(b.x+r*.18f,b.y+r*.25f,r,p);p.color=if(b.cue)Color.rgb(246,225,177) else Color.rgb(248,244,224);c.drawCircle(b.x,b.y,r,p);p.style=Paint.Style.STROKE;p.strokeWidth=1.4f;p.color=Color.rgb(185,177,150);c.drawCircle(b.x,b.y,r,p);p.style=Paint.Style.FILL;if(b.cue){p.color=Color.rgb(150,45,35);c.drawCircle(b.x,b.y,r*.18f,p)}}
 override fun onTouchEvent(e:android.view.MotionEvent):Boolean{val q=cue?:return true;if(moving||q.pocketed)return true;when(e.action){MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE->{dragging=true;aimX=e.x;aimY=e.y;val dx=q.x-e.x;val dy=q.y-e.y;power=min(100f,sqrt(dx*dx+dy*dy)/3f);invalidate()};MotionEvent.ACTION_UP->{if(dragging){val dx=q.x-e.x;val dy=q.y-e.y;val d=sqrt(dx*dx+dy*dy);if(d>15){val speed=min(38f,d*.075f);q.vx=dx/d*speed;q.vy=dy/d*speed;moving=true;scored=false}dragging=false;power=0f}}};return true}
 private fun step(){
  var any=false
  for(b in balls)if(!b.pocketed){b.x+=b.vx;b.y+=b.vy;b.vx*=.989f;b.vy*=.989f;if(abs(b.vx)<.025)b.vx=0f;if(abs(b.vy)<.025)b.vy=0f;if(b.vx!=0f||b.vy!=0f)any=true; cushion(b);checkPocket(b)}
  for(i in 0 until balls.size)for(j in i+1 until balls.size) collide(balls[i],balls[j])
  if(moving&&!any){moving=false;if(!scored)player=3-player; val q=cue!!;if(q.pocketed){q.pocketed=false;q.x=left+(right-left)*.25f;q.y=(top+bottom)/2;q.vx=0f;q.vy=0f;player=3-player};if(s1>=8||s2>=8){val win=if(s1>=8)1 else 2;Toast.makeText(context,"Игрок $win победил! Новая партия.",Toast.LENGTH_LONG).show();reset()}}
 }
 private fun cushion(b:Ball){if(b.pocketed)return;if(b.x-r<left){b.x=left+r;b.vx=abs(b.vx)*.92f};if(b.x+r>right){b.x=right-r;b.vx=-abs(b.vx)*.92f};if(b.y-r<top){b.y=top+r;b.vy=abs(b.vy)*.92f};if(b.y+r>bottom){b.y=bottom-r;b.vy=-abs(b.vy)*.92f}}
 private fun checkPocket(b:Ball){val pts=arrayOf(left to top,(left+right)/2 to top,right to top,left to bottom,(left+right)/2 to bottom,right to bottom);for(pt in pts){val dx=b.x-pt.first;val dy=b.y-pt.second;if(dx*dx+dy*dy<pocket*pocket*.72f){b.pocketed=true;b.vx=0f;b.vy=0f;if(!b.cue){if(player==1)s1++ else s2++;scored=true};return}}}
 private fun collide(a:Ball,b:Ball){if(a.pocketed||b.pocketed)return;val dx=b.x-a.x;val dy=b.y-a.y;val d2=dx*dx+dy*dy;val md=2*r;if(d2<=0||d2>=md*md)return;val d=sqrt(d2);val nx=dx/d;val ny=dy/d;val overlap=md-d;a.x-=nx*overlap/2;b.x+=nx*overlap/2;a.y-=ny*overlap/2;b.y+=ny*overlap/2;val rel=(b.vx-a.vx)*nx+(b.vy-a.vy)*ny;if(rel<0){val imp=-rel*.97f;a.vx-=imp*nx;a.vy-=imp*ny;b.vx+=imp*nx;b.vy+=imp*ny}}
 override fun onAttachedToWindow(){super.onAttachedToWindow();post(loop)}
 private val loop=object:Runnable{override fun run(){step();postDelayed(this,16)}}
}
