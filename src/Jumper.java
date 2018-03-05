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
	/* 画面大小 */
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
		/* 建立控制器，传入分数控制器 */
		mainView=new Controller((Integer score)->addScore(score));
		
		/* 建立及设置主窗体 */
		mainFrame=new JFrame(APP_NAME);
		mainFrame.setSize(new Dimension(WIDTH,HEIGHT));
		mainFrame.setResizable(false);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.getContentPane().setLayout(new BorderLayout());
		mainFrame.getContentPane().add(mainView,BorderLayout.CENTER);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setVisible(true);

		/* 初始化游戏 */
		score=0;
		mainView.initialize();
	}

	/**
	 * 分数控制方法
	 *
	 * @param s 待增加的分数。当s<0时功能为得分清零。
	 */
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
	private static final int SPEED_K=10; //速度缩放系数，用于减慢按压速度
	private static final int ACCELERATION=1; //加速系数，用于控制按压力量

	/**
	 * 初始化Player对象
	 *
	 * @param x 位置坐标x
	 * @param y 位置坐标y
	 * @param r 半径
	 */
	public Player(int x,int y,int r)
	{
		this.x=x;
		this.y=y;
		this.width=r;
		this.height=r;
		this.h=0;
		this.v=0;
	}

	/**
	 * 获得触地点x坐标
	 *
	 * @return 触地点x坐标
	 */
	public int getCenterX()
	{
		return x+width/2;
	}

	/**
	 * 获得触地点y坐标
	 *
	 * @return 触地点y坐标
	 */
	public int getCenterY()
	{
		return y+height;
	}

	/**
	 * 按压Player（步进方法）
	 * 每调用一次即增加力度一次。
	 *
	 * @return 是否已按压至极限
	 */
	public boolean press()
	{
		if(height<=width/3) return true;
		int old_height=height;
		v+=ACCELERATION;
		height=width-v/SPEED_K;
		y-=height-old_height;
		return false;
	}

	/**
	 * 弹起Player
	 * 恢复Player原来形状，并给予初始高度，避免被直接判定为触地。
	 */
	public void bounce()
	{
		y-=width-height;
		height=width;
		h=v;
	}

	/**
	 * 进行空中的位移（步进方法）
	 * 每调用一次即进行一个微小位移。
	 *
	 * @param direction 飞行方向，1或-1
	 */
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
		return false;
	}

	/**
	 * 在给定容器中画出Player。
	 *
	 * @param g 容器的Graphics对象
	 */
	public void draw(Graphics g)
	{
		g.setColor(Color.red);
		g.fillOval(x,y-h/SPEED_K,width,height);
	}
}

class Box
{
	/* r为x轴方向半对角线长度 */
	public int x,y,r;

	/* 方块的大小范围 */
	public static final int MIN_R=25;
	public static final int MAX_R=35;
	public static final int AVE_R=(MIN_R+MAX_R)/2;

	/* 当前步进加分，用于连击加分 */
	private static int reward=1;

	public Box(int x,int y,int r)
	{
		this.x=x;
		this.y=y;
		this.r=r;
	}

	/**
	 * 初始化方块
	 */
	public static void initialize()
	{
		reward=1;
	}

	/**
	 * 获得方块上表面中心x坐标
	 *
	 * @return 上表面中心x坐标
	 */
	public int getCenterX()
	{
		return x+r;
	}

	/**
	 * 获得方块上表面中心y坐标
	 *
	 * @return 上表面中心y坐标
	 */
	public int getCenterY()
	{
		return (int)(y+r*Controller.T); //乘T用于形成空间线段的斜视角长度
	}

	/**
	 * 检测给定坐标是否落在上表面内
	 *
	 * @param x 给定点x坐标
	 * @param y 给定点y坐标
	 * @return 若落在上表面内，返回应得分数；否则返回0。
	 */
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

	/**
	 * 在给定容器中画出方块
	 *
	 * @param g 给定容器的Graphics对象
	 */
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
	/* 重力，控制Player弹起后的下落时长 */
	public static final int GRAVITY=3;
	/* 视角正切值，即同样长度的线段沿y轴放置的视长y与沿x轴放置的视长x之比，即T=y/x */
	public static final double T=0.5;

	/* Player默认位置及大小参数 */
	private static final int PLAYER_R=10;
	private static final int PLAYER_X=(Jumper.WIDTH-PLAYER_R)/2;
	private static final int PLAYER_Y=(int)(Jumper.HEIGHT*0.618-PLAYER_R/2);

	/* 方块距离范围 */
	private static final int MIN_DISTANCE=50;
	private static final int MAX_DISTANCE=100;

	/* 定时间隔，即画面重绘间隔 */
	private static final int INTERVAL=20;

	private Player player;
	private ArrayList<Box> boxes;
	private Timer timer;

	/* 分数控制器，负责控制窗体标题栏分数的显示 */
	private Consumer<Integer> scoreController;
	
	/* 游戏的状态变量 */
	private boolean playing;	//是否在定时重绘中（仅失败后等待重新开始时不需重绘）
	private boolean failed;		//是否已失败且未重新开始
	private boolean pressing;	//是否在按压状态中
	private boolean moving;		//是否在飞行状态中
	private int direction;		//当前飞行方向，仅取1（向右）或-1（向左）

	/**
	 * 初始化游戏控制器
	 *
	 * @param scoreController 窗体分数控制器
	 */
	public Controller(Consumer<Integer> scoreController)
	{
		/* 注册窗体分数控制器 */
		this.scoreController=scoreController;

		/* 注册鼠标监听器 */
		addMouseListener(new MouseAdapter()
				{
					@Override
					public void mousePressed(MouseEvent e)
					{
						/* 当鼠标点击时请求焦点，否则键盘监听器无法成功监听 */
						requestFocus();

						/* 检测按压事件 */
						if(playing&&!moving&&e.getButton()==e.BUTTON1)
							pressing=true;
					}

					public void mouseReleased(MouseEvent e)
					{
						/* 检测弹起事件 */
						if(playing&&!moving&&e.getButton()==e.BUTTON1)
						{
							pressing=false;
							moving=true;
							player.bounce();
						}
					}
				});

		/* 注册键盘监听器 */
		addKeyListener(new KeyAdapter()
				{
					@Override
					public void keyPressed(KeyEvent e)
					{
						/* 检测重新开始游戏事件 */
						if(!playing&&e.getKeyCode()==KeyEvent.VK_ENTER)
						{
							timer.stop();
							initialize();
						}
					}
				});

		/* 设置游戏定时器 */
		timer=new Timer(INTERVAL,new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						/* 如果正在可重绘状态，对游戏状态进行步进，并重绘画面 */
						if(playing)
						{
							step();
							updateView();
						}
					}
				});
	}

	/**
	 * 重绘画面
	 */
	public void updateView()
	{
		Graphics g=this.getGraphics();

		/* 清除当前画面 */
		g.clearRect(0,0,Jumper.WIDTH,Jumper.HEIGHT);

		/* 绘制所有方块 */
		for(Box box:boxes)
		{
			box.draw(g);
		}
		/* 当游戏并未失败时，绘制Player */
		if(!failed) player.draw(g);
		else
		{
			/* 若游戏已经失败，则使Player消失（不绘制Player），
			 * 并绘制重新开始游戏提示。
			 */
			g.setColor(Color.black);
			g.setFont(new Font("Consolas",Font.PLAIN,15));
			g.drawString("Press Enter to restart.",2,15);
		}
	}

	/**
	 * 初始化游戏
	 */
	public void initialize()
	{
		/* 初始化状态变量 */
		playing=true;
		pressing=false;
		moving=false;
		failed=false;
		direction=1;

		/* 初始化游戏分数 */
		scoreController.accept(-1);

		/* 初始化连击得分 */
		Box.initialize();

		/* 创建新的Player */
		player=new Player(PLAYER_X,PLAYER_Y,PLAYER_R);

		/* 初始化方块列表，并添加初始方块 */
		boxes=new ArrayList<Box>();
		boxes.add(new Box(player.getCenterX()-Box.AVE_R,(int)(player.getCenterY()-Box.AVE_R*T),Box.AVE_R));
		boxes.add(generateBox());

		/* 游戏定时器开始工作 */
		timer.start();
	}

	/**
	 * 游戏状态步进方法
	 * 每次定时器间隔都会被调用，从而更新游戏状态。
	 */
	private void step()
	{
		if(failed)
		/* 失败状态处理 */
		{
			playing=false;
		}
		else if(pressing)
		/* 按压状态处理 */
		{
			/* 加大按压力度 */
			player.press();
		}
		else if(moving)
		/* 飞行状态处理 */
		{
			/* 对Player飞行进行步进 */
			if(player.step(direction))
			/* 进行落地处理 */
			{
				moving=false;

				/* 判断是否进入下一方块 */
				int score=boxes.get(boxes.size()-1).check(player.getCenterX(),player.getCenterY());
				if(score>0)
				{
					scoreController.accept(score);
					boxes.add(generateBox());
					return;
				}

				/*判断是否触地*/
				boolean flag=false;
				for(Box box:boxes)
				{
					if(box.check(player.getCenterX(),player.getCenterY())>0)
					{
						flag=true;
						break;
					}
				}

				/* 若未成功触地则失败 */
				if(!flag) fail();
			}
			else
			/* 进行飞行处理 */
			{
				/* 向飞行的相反方向移动所有方块 */
				for(Box box:boxes)
				{
					box.x-=Player.FLYING_SPEED*direction;
					box.y+=Player.FLYING_SPEED*T;
				}
			}
		}
	}

	/**
	 * 生成随机方块
	 *
	 * @return 已生成的新随机方块
	 */
	private Box generateBox()
	{
		/* 生成一个随机方向 */
		direction=(int)(Math.random()*2)*2-1;

		/* 生成随机位置及大小 */
		int distance=(int)(Math.random()*(MAX_DISTANCE-MIN_DISTANCE)+MIN_DISTANCE);
		int centerX=player.getCenterX()+direction*distance;
		int centerY=(int)(player.getCenterY()-distance*T);
		int r=(int)(Math.random()*(Box.MAX_R-Box.MIN_R)+Box.MIN_R);

		/* 创建并返回新的Box对象 */
		return new Box(centerX-r,(int)(centerY-r*T),r);
	}

	/**
	 * 游戏失败（可扩展）
	 */
	private void fail()
	{
		failed=true;
	}
}
