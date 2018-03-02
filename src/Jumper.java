import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.util.ArrayList;
import javax.swing.Timer;
import java.util.function.Consumer;

public class Jumper
{
	public static final int WIDTH=300;
	public static final int HEIGHT=400;

	private static final String APP_NAME="Jumper";
	JFrame mainFrame;
	Controller mainView;

	private int score;

	public static void main(String[] args)
	{
		Jumper jumper=new Jumper();
	}

	public Jumper()
	{
		mainFrame=new JFrame(APP_NAME);
		mainView=new Controller((Integer score)->addScore(score));
		mainFrame.setSize(new Dimension(WIDTH,HEIGHT));
		mainFrame.setResizable(false);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.getContentPane().setLayout(new BorderLayout());
		mainFrame.getContentPane().add(mainView,BorderLayout.CENTER);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setVisible(true);
		score=0;
		mainView.initialize();
	}

	private void addScore(int s)
	{
		if(s<0) score=0;
		else score+=s;
		mainFrame.setTitle(APP_NAME+" - Score:"+score);
	}
}

class Player
{
	public int x,y,width,height,h;
	private int v;
	
	public static final int FLYING_SPEED=5;
	private static final int SPEED_K=10;
	private static final int ACCELERATION=1;

	public Player(int x,int y,int r)
	{
		this.x=x;
		this.y=y;
		this.width=r;
		this.height=r;
		this.h=0;
		this.v=0;
	}

	public int getCenterX()
	{
		return x+width/2;
	}
	public int getCenterY()
	{
		return y+height;
	}

	public boolean press()
	{
		if(height<=width/3) return true;
		int old_height=height;
		v+=ACCELERATION;
		height=width-v/SPEED_K;
		y-=height-old_height;
		return false;
	}

	public void bounce()
	{
		y-=width-height;
		height=width;
		h=v;
		//System.out.println("h="+h+"\tv="+v);
	}

	public boolean step(int direction)
	{
		if(h<=0) return true;
		v-=Controller.GRAVITY;
		h+=v;
		if(h<=0)
		{
			h=0;
			v=0;
		}
		//System.out.println("h="+h+"\tv="+v);
		return false;
	}

	public void draw(Graphics g)
	{
		g.setColor(Color.red);
		g.fillOval(x,y-h/SPEED_K,width,height);
	}
}

class Box
{
	public int x,y,r;

	public static final int MIN_R=25;
	public static final int MAX_R=35;
	public static final int AVE_R=(MIN_R+MAX_R)/2;

	private static int reward=1;

	public Box(int x,int y,int r)
	{
		this.x=x;
		this.y=y;
		this.r=r;
	}

	public static void initialize()
	{
		reward=1;
	}

	public int getCenterX()
	{
		return x+r;
	}
	public int getCenterY()
	{
		return (int)(y+r*Controller.T);
	}

	public int check(int x,int y)
	{
		int e=(int)(Math.abs(x-getCenterX())+Math.abs(y-getCenterY())/Controller.T+0.5);
		if(e==0)
		{
			reward*=2;
			return reward;
		}
		else if(e<r)
		{
			reward=1;
			return reward;
		}
		else
		{
			reward=1;
			return 0;
		}
	}

	public void draw(Graphics g)
	{
		g.setColor(Color.blue);
		g.drawLine(x,getCenterY(),getCenterX(),y);
		g.drawLine(x+2*r,getCenterY(),getCenterX(),y);
		g.drawLine(x,getCenterY(),getCenterX(),(int)(y+2*r*Controller.T));
		g.drawLine(x+2*r,getCenterY(),getCenterX(),(int)(y+2*r*Controller.T));
	}
}

class Controller extends JPanel
{
	public static final int GRAVITY=3;
	public static final double T=0.5; //tan

	private static final int PLAYER_R=10;
	private static final int PLAYER_X=(Jumper.WIDTH-PLAYER_R)/2;
	private static final int PLAYER_Y=(int)(Jumper.HEIGHT*0.618-PLAYER_R/2);
	private static final int MIN_DISTANCE=50;
	private static final int MAX_DISTANCE=100;
	private static final int INTERVAL=20;

	private Player player;
	private ArrayList<Box> boxes;
	private Timer timer;
	private Consumer<Integer> scoreController;
	
	private boolean playing;
	private boolean failed;
	private boolean pressing;
	private boolean moving;
	private int direction;

	public Controller(Consumer<Integer> scoreController)
	{
		this.scoreController=scoreController;
		addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						requestFocus();
						if(playing&&!moving&&e.getButton()==e.BUTTON1)
							pressing=true;
					}

					public void mouseReleased(MouseEvent e)
					{
						if(playing&&!moving&&e.getButton()==e.BUTTON1)
						{
							pressing=false;
							moving=true;
							player.bounce();
						}
					}
				});
		addKeyListener(new KeyAdapter()
				{
					public void keyPressed(KeyEvent e)
					{
						//System.out.println("playing:"+playing+"\tkeyCode="+e.getKeyCode());
						if(!playing&&e.getKeyCode()==KeyEvent.VK_ENTER)
						{
							//System.out.println("restart.");
							timer.stop();
							initialize();
						}
					}
				});
		timer=new Timer(INTERVAL,new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						if(playing)
						{
							step();
							updateView();
						}
					}
				});
	}

	public void updateView()
	{
		Graphics g=this.getGraphics();
		g.clearRect(0,0,Jumper.WIDTH,Jumper.HEIGHT);
		for(Box box:boxes)
		{
			/*if(box.x>WIDTH||box.x+box.r<0||box.y>HEIGHT)
				boxes.remove(box);
			else*/
				box.draw(g);
		}
		if(!failed) player.draw(g);
		else
		{
			//System.out.println("failed.");
			g.setColor(Color.black);
			g.setFont(new Font("Consolas",Font.PLAIN,15));
			g.drawString("Press Enter to restart.",2,15);
		}
	}

	public void initialize()
	{
		playing=true;
		pressing=false;
		moving=false;
		failed=false;
		scoreController.accept(-1);
		direction=1;
		player=new Player(PLAYER_X,PLAYER_Y,PLAYER_R);
		Box.initialize();
		boxes=new ArrayList<Box>();
		boxes.add(new Box(player.getCenterX()-Box.AVE_R,(int)(player.getCenterY()-Box.AVE_R*T),Box.AVE_R));
		boxes.add(generateBox());
		timer.start();
	}

	private void step()
	{
		//System.out.println("player=("+player.x+","+player.y+")");
		if(failed)
		{
			playing=false;
		}
		else if(pressing)
		{
			player.press();
		}
		else if(moving)
		{
			if(player.step(direction))
			{
				moving=false;
				int score=boxes.get(boxes.size()-1).check(player.getCenterX(),player.getCenterY());
				if(score>0)
				{
					scoreController.accept(score);
					boxes.add(generateBox());
					return;
				}
				boolean flag=false;
				for(Box box:boxes)
				{
					if(box.check(player.getCenterX(),player.getCenterY())>0)
					{
						flag=true;
						break;
					}
				}
				if(!flag) fail();
			}
			else
			{
				for(Box box:boxes)
				{
					box.x-=Player.FLYING_SPEED*direction;
					box.y+=Player.FLYING_SPEED*T;
				}
			}
		}
	}

	private Box generateBox()
	{
		direction=(int)(Math.random()*2)*2-1; //get a random number either 1 or -1
		int distance=(int)(Math.random()*(MAX_DISTANCE-MIN_DISTANCE)+MIN_DISTANCE);
		//System.out.println(distance);
		int centerX=player.getCenterX()+direction*distance;
		int centerY=(int)(player.getCenterY()-distance*T);
		int r=(int)(Math.random()*(Box.MAX_R-Box.MIN_R)+Box.MIN_R);
		return new Box(centerX-r,(int)(centerY-r*T),r);
	}

	private void fail()
	{
		failed=true;
	}
}
