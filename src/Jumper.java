import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import javax.swing.Timer;

public class Jumper
{
	public static final double T=0.5; //tan
	private static final int MIN_DISTANCE=20;
	private static final int MAX_DISTANCE=100;
	private static final int WIDTH=300;
	private static final int HEIGHT=400;
	JFrame mainFrame;
	Controller mainView;

	public static void main(String[] args)
	{
		Jumper jumper=new Jumper();
	}

	public Jumper()
	{
		mainFrame=new JFrame("Jumper");
		mainView=new Controller();
		mainFrame.setSize(new Dimension(WIDTH,HEIGHT));
		mainFrame.setLayout(null);
		mainFrame.add(mainView);
		mainView.setBounds(0,0,WIDTH,HEIGHT);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}

class Controller extends JLabel
{
	private static final int PLAYER_INIT_X=100;
	private static final int PLAYER_INIT_Y=300;
	private static final int PLAYER_INIT_R=10;

	private Player player;
	private ArrayList<Box> boxes;
	private Timer timer;
	private boolean pressing;
	private boolean moving;
	public Controller()
	{
		addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						if(!player.isFloating()&&e.getButton()==e.BUTTON1)
							pressing=true;
					}

					public void mouseReleased(MouseEvent e)
					{
						if(!player.isFloating()&&e.getButton()==e.BUTTON1)
							pressing=false;
					}
				});
		timer=new Timer(20,new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						//TODO:pressing&moving
						updateView();
					}
				});
		initialize();
	}

	private void initialize()
	{
		pressing=false;
		player=new Player(PLAYER_INIT_X,PLAYER_INIT_Y,PLAYER_INIT_R);
		//TODO:add two initial boxes.
	}

	public void updateView()
	{
		player.draw();
		for(Box box:boxes)
		{
			if(box.x>WIDTH||box.x+box.width<0||box.y>HEIGHT)
				boxes.remove(box);
			else
				box.draw();
		}
	}

	private Box generateBox()
	{
		//TODO
	}

	private void fail()
	{
		//TODO
	}
}

class Box
{
	public int x,y,r;

	public Box(int x,int y,int r)
	{
		this.x=x;
		this.y=y;
		this.r=r;
	}

	public int getCenterX()
	{
		return x+r;
	}
	public int getCenterY()
	{
		return y+r*Jumper.T;
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

class Player
{
	public int x,y,width,height,h;
	private boolean floating;
	private int v;
	
	public static final int FLYING_SPEED=2;
	private static final int PRESS_SPEED=1;

	public Player(int x,int y,int r)
	{
		this.x=x;
		this.y=y;
		this.width=r;
		this.height=r;
		this.h=0;
		this.v=0;
		floating=false;
	}

	public int getCenterX()
	{
		return x+width/2;
	}
	public int getCenterY()
	{
		return y+height;
	}

	public boolean isFloating()
	{
		return floating;
	}

	public boolean press()
	{
		height/=PRESS_SPEED;
		return height<width/2;
	}

	public void draw(Graphics g)
	{
		g.setColor(Color.red);
		g.drawOval(x,y,width,height);
	}
}
