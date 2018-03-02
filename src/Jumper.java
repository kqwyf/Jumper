import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Color;
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
	private static final int WIDTH=300;
	private static final int HEIGHT=400;
	private static final String APP_NAME="Jumper";
	JFrame mainFrame;
	Controller mainView;

	public static void main(String[] args)
	{
		Jumper jumper=new Jumper();
	}

	public Jumper()
	{
		mainFrame=new JFrame(APP_NAME);
		mainView=new Controller((int score)->setScore(score));
		mainFrame.setSize(new Dimension(WIDTH,HEIGHT));
		mainFrame.setLayout(null);
		mainFrame.add(mainView);
		mainView.setBounds(0,0,WIDTH,HEIGHT);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private void setScore(int score)
	{
		mainFrame.setTitle(APP_NAME+" - Score:"+score);
	}
}

class Player
{
	public int x,y,width,height,h;
	private int v;
	
	public static final int FLYING_SPEED=2;
	private static final double PRESS_SPEED=1.2;
	private static final int SPEED_K=5;

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
		int old_height=height;
		height/=PRESS_SPEED;
		y-=height-old_height;
		return return height==old_height;
	}

	public void bounce()
	{
		v=SPEED_K*(height-width);
		y-=width-height;
		height=width;
	}

	public boolean step(int direction)
	{
		if(h<=0) return true;
		h+=v;
		if(h<0) h=0;
		v-=Controller.GRAVITY;
		return false;
	}

	public void draw(Graphics g)
	{
		g.setColor(Color.red);
		g.drawOval(x,y-h,width,height);
	}
}

class Box
{
	public int x,y,r;

	private static int reward=1;

	private static final int MIN_R=10;
	private static final int MAX_R=20;
	private static final int AVE_R=(MIN_R+MAX_R)/2;

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
		return y+r*Jumper.T;
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
		g.drawLine(x,getCenterY(),getCenterX(),y+r*Jumper.T);
		g.drawLine(x+2*r,getCenterY(),getCenterX(),y+r*Jumper.T);
	}
}

class Controller extends JLabel
{
	private static final int PLAYER_R=10;
	private static final int PLAYER_X=(Jumper.WIDTH-PLAYER_R)/2;
	private static final int PLAYER_Y=Jumper.HEIGHT*0.618-PLAYER_R/2;
	private static final double T=0.5; //tan
	private static final int MIN_DISTANCE=20;
	private static final int MAX_DISTANCE=100;
	private static final int GRAVITY=1;

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
						if(!playing&&e.getKeyCode()==VK_ENTER)
						{
							playing=true;
						}
					}
				});
		timer=new Timer(20,new ActionListener()
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
		initialize();
		Box.initialize();
	}

	public void updateView()
	{
		Graphics g=this.getGraphics();
		g.clearRect(0,0,Jumper.WIDTH,Jumper.HEIGHT);
		if(!failed) player.draw(g);
		for(Box box:boxes)
		{
			if(box.x>WIDTH||box.x+box.width<0||box.y>HEIGHT)
				boxes.remove(box);
			else
				box.draw(g);
		}
	}

	private void initialize()
	{
		playing=true;
		pressing=false;
		moving=false;
		direction=1;
		player=new Player(PLAYER_X,PLAYER_Y,PLAYER_R);
		boxes=new ArrayList<Box>();
		boxes.add(new Box(player.x-Box.AVE_R,player.y-Box.AVE_R*T));
		boxes.add(generateBox());
	}

	private void step()
	{
		if(failed)
		{
			getGraphics().drawString("Press Enter to restart.");
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
					generateBox();
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
		int centerX=player.x+direction*distance;
		int centerY=player.y-distance*T;
		int r=(int)(Math.random()*(MAX_R-MIN_R)+MIN_R);
		return new Box(centerX-r,centerY-r*T,r);
	}

	private void fail()
	{
		failed=true;
	}
}
