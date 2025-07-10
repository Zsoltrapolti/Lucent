
import java.util.*;
import java.awt.*;
import java.awt.event.*;


class MoveDescr { // datatype
  private byte col; // 1-7

  private MoveDescr( int c ){ col = (byte)c; }
    
  static final MoveDescr one = new MoveDescr(1);
  static final MoveDescr two = new MoveDescr(2);
  static final MoveDescr thr = new MoveDescr(3);
  static final MoveDescr fou = new MoveDescr(4);
  static final MoveDescr fiv = new MoveDescr(5);
  static final MoveDescr six = new MoveDescr(6);
  static final MoveDescr sev = new MoveDescr(7);
  static final MoveDescr dummy = new MoveDescr(0);

  byte getCol() { return col; }
}


class MoveOutcome { // datatype

  private MoveOutcome(){}
    
  static final MoveOutcome won     = new MoveOutcome();
  static final MoveOutcome ok      = new MoveOutcome();
  static final MoveOutcome illegal = new MoveOutcome();
}


class Position { // datatype
// Position beinhaltet, wer am Zug ist bzw. wer gewonnen hat.
  static final byte empty = 0; 
  static final byte white = 1;
  static final byte black = 2;
  static final byte whiteNext = 3;
  static final byte blackNext = 4;
  static final byte whiteWon  = 5;
  static final byte blackWon  = 6;

  private byte[] board;
  private byte moveState;

  Position( byte[] b, byte next ) {
    // (b,next) has to be a correct position
    board = new byte[42];
    System.arraycopy(b,0,board,0,42);
    moveState =  next;
  }
  
  byte get(int c, int r) {
    return board[6*(c-1)+r-1];
  }
  byte next() {
    return moveState;
  }
}


interface BoardObserver
{
  void stateChanged( ObservableBoard b );
}


interface ObservableBoard
{
  void register( BoardObserver obs );
  Position currentPosition();
} // moves are not allowed


class Board
  implements ObservableBoard
{
  private byte[] board = new byte[42];
  private byte[] topincol = new byte[8];
  private byte moveState = Position.whiteNext;
  private ArrayList observers = new ArrayList();
  
  private byte get(int c, int r) {
    return board[6*(c-1)+r-1];
  }

  public Position currentPosition() {
    return new Position(board,moveState);
  }
  
  MoveOutcome move( MoveDescr md ) {
    byte c = md.getCol();
    if(   moveState == Position.whiteWon
       || moveState == Position.blackWon
       || topincol[c] == 6 ) {
      return MoveOutcome.illegal;
    } else {
      topincol[c]++;
      byte colour = ( moveState == Position.whiteNext) ?
                      Position.white
                    : Position.black;
      board[ 6*(c-1) + topincol[c]-1 ] = colour;
      if( checkwin(colour) ) {
        if( colour == Position.white ) {
          moveState = Position.whiteWon;
        } else {
          moveState = Position.blackWon;
        }
        notifyObservers();
        return MoveOutcome.won;
      } else {
        if( colour == Position.white ) {
          moveState = Position.blackNext;
        } else {
          moveState = Position.whiteNext;
        }
        notifyObservers();
        return MoveOutcome.ok;
      }
    }
  }

  private void notifyObservers() {
    Iterator it = observers.iterator();
    while( it.hasNext() ) {
      BoardObserver obs = (BoardObserver)it.next();
      obs.stateChanged( this );
    }
  }
  
  public void register( BoardObserver obs ){
    observers.add(obs);
  }

  private boolean checkwin( byte colour ) {
    for( int ic = 1; ic <= 4; ic++ ) {
      for( int ir = 1; ir <= 3; ir++ ) {
        if(  (  get(ic,ir) == colour 
             && get(ic+1,ir) == colour 
             && get(ic+2,ir) == colour 
             && get(ic+3,ir) == colour )
          || (  get(ic,ir) == colour
             && get(ic,ir+1) == colour 
             && get(ic,ir+2) == colour 
             && get(ic,ir+3) == colour )
          || (  get(ic,ir) == colour
             && get(ic+1,ir+1) == colour 
             && get(ic+2,ir+2) == colour 
             && get(ic+3,ir+3) == colour )
             ) return true;
      }
    }
    for( int ic = 1; ic <= 4; ic++ ) {
      for( int ir = 4; ir <= 6; ir++ ) {
        if(  (  get(ic,ir) == colour 
             && get(ic+1,ir) == colour 
             && get(ic+2,ir) == colour 
             && get(ic+3,ir) == colour )
             ) return true;
      }
    }
    for( int ic = 5; ic <= 7; ic++ ) {
      for( int ir = 1; ir <= 3; ir++ ) {
        if(  (  get(ic,ir) == colour
             && get(ic,ir+1) == colour 
             && get(ic,ir+2) == colour 
             && get(ic,ir+3) == colour )
             ) return true;
      }
    }
    return false;
  }
  
}


class GameControler extends Thread {
  private Board theBoard;
  private Player white;
  private Player black;
  private boolean whiteTurn = true;
  private boolean running = false;
  private boolean swapReqest = false;

  void putTheBoard( Board b ) { theBoard = b; }

  void putPlayer( Player p, boolean asWhite ) {
    if( asWhite ) {
      white = p;
    } else {
      black = p;
    }
  }
  
  boolean whiteToMove() { return whiteTurn; }

  public void run() {
    MoveDescr mvd;
    while( true ) {
      synchronized( this ) {
        if( swapReqest ) {
          Player tmp = white;
          white = black;
          black = tmp;
          System.out.println("Players swapped");
          swapReqest = false;
        }
      }
      if( whiteTurn ) {
        mvd = white.draw();
      } else {
        mvd = black.draw();
      }
      synchronized( this ) {
        if( swapReqest ) { // move will not be executed
          Player tmp = white;
	  white = black; 
          black = tmp;
          System.out.println("Players swapped");
          swapReqest = false;
        } else { // execute move
	  MoveOutcome moc = theBoard.move(mvd);
          if( moc == MoveOutcome.ok ) {
	     whiteTurn = !whiteTurn;
          } else if( moc == MoveOutcome.won ) {
	    break;
          } else {
	    System.out.println("Illegal move!");
          }
        }
      }
    }
  }

  synchronized void  startGame() {
    if( !running ) { // and initialized
      running = true;
      start();
    }
  }
  
  synchronized void swapSides() {
    swapReqest = true;
  }
}

  
interface Player 
{
  MoveDescr draw();
}


class ComputerPlayer 
  implements Player
{
  private ObservableBoard theBoard;
  private int last = 3;
  
  void putTheBoard( ObservableBoard b ) { theBoard = b; }

  public MoveDescr draw() {
    int count = 0;
    last = (last+5)%7 +1;
    Position curpos = theBoard.currentPosition();
    try{ Thread.sleep( 2000 ); } catch( InterruptedException ie ) {}
    while( curpos.get(last,6) != Position.empty && count < 7 ) {
      last = (last+5)%7 +1;
      count ++;
    }
    switch( last ) {
    case 1: return MoveDescr.one;
    case 2: return MoveDescr.two;
    case 3: return MoveDescr.thr;
    case 4: return MoveDescr.fou;
    case 5: return MoveDescr.fiv;
    case 6: return MoveDescr.six;
    case 7: return MoveDescr.sev;
    default: return MoveDescr.dummy;
    }
  }
}

class AdapterPlayer
  implements Player
{
  private GameGUI theGUI;
  private MoveDescr theMove;
 
  void putTheGUI( GameGUI g ) { theGUI = g; }

  public synchronized MoveDescr draw() {
    theGUI.enableMove();
    try{
      wait();
    } catch( InterruptedException ie ) {
      return MoveDescr.dummy;
    }
    return theMove;  
  }

  synchronized void notifyMove( MoveDescr mvd ) {
    theMove = mvd;
    notify();
  }
}


class GUIButton
  extends Button
  implements ActionListener
{
  // void klick() from extern
 
  GUIButton( String s ) {
    super( s );
    addActionListener( this );
  }
  public void actionPerformed( ActionEvent e ) {}
}


class GameGUI
  extends  Frame
  implements BoardObserver           
{
  private GameControler gc;
  private AdapterPlayer thePlayer;

  private Label[] fields;
  private Button column1;
  private Button column2;
  private Button column3;
  private Button column4;
  private Button column5;
  private Button column6;
  private Button column7;
  private Button startGame;
  private Button swapSides;
  private Label  whosNext;

  GameGUI( GameControler g, AdapterPlayer pl ) {
    gc = g;
    thePlayer = pl;

    addWindowListener( new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          System.exit ( 0 );
        }
      } );
      
    setLayout( new BorderLayout() );
    Panel p = new Panel();
    p.setLayout( new GridLayout(7,7) );
    fields = new Label[42];
    for( int r = 6; r>=1; r-- ) {
      for( int c = 1; c<=7; c++ ) {
        Label lb = new Label("O",Label.CENTER);
        lb.setBackground( Color.gray );
        fields[6*(c-1)+r-1] = lb;
        p.add(lb);
      }
    }
    column1 = new GUIButton("1") {
        public void actionPerformed( ActionEvent e ) {
          GameGUI.this.setEnabledMoveButtons( false );
          thePlayer.notifyMove( MoveDescr.one );
        } };
    column2 = new GUIButton("2") {
        public void actionPerformed( ActionEvent e ) {
          GameGUI.this.setEnabledMoveButtons( false );
          thePlayer.notifyMove( MoveDescr.two );
        } };
    column3 = new GUIButton("3") {
        public void actionPerformed( ActionEvent e ) {
          GameGUI.this.setEnabledMoveButtons( false );
          thePlayer.notifyMove( MoveDescr.thr );
        } };
    column4 = new GUIButton("4") {
        public void actionPerformed( ActionEvent e ) {
          GameGUI.this.setEnabledMoveButtons( false );
          thePlayer.notifyMove( MoveDescr.fou );
        } };
    column5 = new GUIButton("5") {
        public void actionPerformed( ActionEvent e ) {
          GameGUI.this.setEnabledMoveButtons( false );
          thePlayer.notifyMove( MoveDescr.fiv );
        } };
    column6 = new GUIButton("6") {
        public void actionPerformed( ActionEvent e ) {
          GameGUI.this.setEnabledMoveButtons( false );
          thePlayer.notifyMove( MoveDescr.six );
        } };
    column7 = new GUIButton("7") {
        public void actionPerformed( ActionEvent e ) {
          GameGUI.this.setEnabledMoveButtons( false );
          thePlayer.notifyMove( MoveDescr.sev );
        } };
    setEnabledMoveButtons( false );
    p.add( column1 );
    p.add( column2 );
    p.add( column3 );
    p.add( column4 );
    p.add( column5 );
    p.add( column6 );
    p.add( column7 );
    add( p, "Center" );
    
    Panel bp = new Panel();
    startGame = new GUIButton("start") {
        public void actionPerformed( ActionEvent e ) {
          setEnabled( false );
          gc.startGame();
        } };
    swapSides = new GUIButton("swap") {
        public void actionPerformed( ActionEvent e ) {
          GameGUI.this.setEnabledMoveButtons( false );
          gc.swapSides();
          thePlayer.notifyMove( MoveDescr.dummy );
        } };
    bp.add( startGame );
    bp.add( swapSides );
    whosNext = new Label("white to play");
    bp.add( whosNext );
    add( bp, "South" );

    setSize(400,300);
    setLocation(100,200);
    setVisible( true );
  }
  
  void enableMove() {
    setEnabledMoveButtons( true );
  }

  public void stateChanged( ObservableBoard b ) {
    Position pos = b.currentPosition();
    for( int c = 1; c<=7; c++ ) {
      for( int r = 1; r<=6; r++ ) {
        switch( pos.get(c,r) ) {
        case Position.empty:
          fields[6*(c-1)+r-1].setBackground( Color.gray );
          break;
        case Position.white:
          fields[6*(c-1)+r-1].setBackground( Color.white );
          break;
        case Position.black:
          fields[6*(c-1)+r-1].setBackground( Color.blue );
          break;
        }
      }
    }
    switch( pos.next() ) {
    case Position.whiteNext:
      whosNext.setText("white to play");
      break;
    case Position.blackNext:
      whosNext.setText("blue to play");
      break;
    case Position.whiteWon:
      whosNext.setText("white won");
      break;
    case Position.blackWon:
      whosNext.setText("blue won");
    }
    repaint();
  }
  
  private void setEnabledMoveButtons( boolean b ) {
    column1.setEnabled( b );
    column2.setEnabled( b );
    column3.setEnabled( b );
    column4.setEnabled( b );
    column5.setEnabled( b );
    column6.setEnabled( b );
    column7.setEnabled( b );
    repaint();
  }  
  
}


public class FourWins
{
  public static void main(String argv[]) {
    
    Board theBoard = new Board();
    
    ComputerPlayer cp = new ComputerPlayer();
    cp.putTheBoard( theBoard );
    
    AdapterPlayer ap = new AdapterPlayer();
    
    GameControler gc = new GameControler();
    gc.putTheBoard( theBoard );
    gc.putPlayer( cp, false );
    gc.putPlayer( ap, true );
    
    GameGUI theGUI = new GameGUI(gc,ap);
    theBoard.register( theGUI );
    
    ap.putTheGUI(theGUI);
  }
}
