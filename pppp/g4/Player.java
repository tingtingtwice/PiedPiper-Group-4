package pppp.g4;

import pppp.sim.Point;
import pppp.sim.Move;

import java.util.*;

public class Player implements pppp.sim.Player {

	// see details below
	private int id = -1;
	private int side = 0;
	private int[] pos_index = null;
	private Point[][] pos = null;
	private Point[] random_pos = null;
	private Random gen = new Random();
	private int dst_no = 0;
	private int total_regions = 0;
	Boolean[] completed_sweep = null;
    private Cell[] grid = null;
    private static double density_threshold = 0.005;
    private Boolean sparse_flag = false;
    Map<Integer, Point> piper_to_cell = null;
    int tick = 0;
    Point our_gate = null;

	// create move towards specified destination
	private static Move move(Point src, Point dst, boolean play) {
		double dx = dst.x - src.x;
		double dy = dst.y - src.y;
		double length = Math.sqrt(dx * dx + dy * dy);
		double limit = play ? 0.1 : 0.5;
		if (length > limit) {
			dx = (dx * limit) / length;
			dy = (dy * limit) / length;
		}
		return new Move(dx, dy, play);
	}

	// generate point after negating or swapping coordinates
	private static Point point(double x, double y,
							   boolean neg_y, boolean swap_xy) {
		if (neg_y) y = -y;
		return swap_xy ? new Point(y, x) : new Point(x, y);
	}



    double getSweepRadius(Point[] rats, Point[] boundaries, int id){
        double radius = side/1.8;
        int sum_strip1 = 0;
        int sum_strip2 = 0;
        int sum_rem = 0;
        double avg = 0.0;

        for(Point rat: rats)
        {
            if (id == 0 || id == 2)
            {
                //Only consider y axis boundaries
                if ((rat.y >= Math.min(boundaries[0].y, boundaries[1].y)) && ((rat.y < Math.max(boundaries[0].y, boundaries[1].y))))
                    {sum_strip1 += 1;}
                else if ((rat.y >= Math.min(boundaries[1].y, boundaries[2].y)) && ((rat.y < Math.max(boundaries[1].y, boundaries[2].y))))
                    {sum_strip2 += 1;}
                else
                    {sum_rem += 1;}               
            }
            else if (id == 1 || id == 3)
            {
                // id = 1 or 3 | Considering X-axis only
                if ((rat.x > Math.min(boundaries[0].x, boundaries[1].x)) && ((rat.x < Math.max(boundaries[0].x, boundaries[1].x))))
                    {sum_strip1 += 1;}  
                else if ((rat.x > Math.min(boundaries[1].x, boundaries[2].x)) && ((rat.x < Math.max(boundaries[1].x, boundaries[2].x))))
                    {sum_strip2 += 1;}
                else
                    {sum_rem += 1;  }     
            } 
        }
        avg = (sum_strip1 + sum_strip2+ sum_rem)/2;
        if (sum_strip2 > avg )
        {
            radius = side/2.5;
        }
        else if (sum_strip1 >avg) {
            radius = side/4;
        }
        System.out.println("Total rats : "+rats.length+ " | strip 1 : "+ sum_strip1 + " | strip 2 : "+ sum_strip2 + " | remaining "+ sum_rem + " | RADIUSLinkedFolder : "+radius);
        return radius;
    }
    double getSweepRadius2(Point[] rats, Point[] boundaries, int id){
        double radius = side/3;
        int sum_strip1 = 0;
        int sum_strip2 = 0;
        int sum_strip3 = 0;
        int sum_rem = 0;
        double avg = 0.0;

        for(Point rat: rats)
        {
            if (id == 0 || id == 2)
            {
                //Only consider y axis boundaries
                if ((rat.y >= Math.min(boundaries[0].y, boundaries[1].y)) && ((rat.y < Math.max(boundaries[0].y, boundaries[1].y))))
                    {sum_strip1 += 1;}
                else if ((rat.y >= Math.min(boundaries[1].y, boundaries[2].y)) && ((rat.y < Math.max(boundaries[1].y, boundaries[2].y))))
                    {sum_strip2 += 1;}
                else if ((rat.y >= Math.min(boundaries[2].y, boundaries[3].y)) && ((rat.y < Math.max(boundaries[2].y, boundaries[3].y))))
                     {sum_strip3 += 1;}
                else
                    {sum_rem += 1;}  //between 75 to 100 all rats we leave this as too risky area..             
            }
            else if (id == 1 || id == 3)
            {
                // id = 1 or 3 | Considering X-axis only
                if ((rat.x > Math.min(boundaries[0].x, boundaries[1].x)) && ((rat.x < Math.max(boundaries[0].x, boundaries[1].x))))
                    {sum_strip1 += 1;}  
                else if ((rat.x > Math.min(boundaries[1].x, boundaries[2].x)) && ((rat.x < Math.max(boundaries[1].x, boundaries[2].x))))
                    {sum_strip2 += 1;}
                else if ((rat.x > Math.min(boundaries[2].x, boundaries[3].x)) && ((rat.x < Math.max(boundaries[2].x, boundaries[3].x))))
                    {sum_strip3 += 1;}
                else
                    {sum_rem += 1;  }  //between 75 to 100 all rats we leave this as too risky area..     
            } 
        }
        avg = (sum_strip1 + sum_strip2 + sum_strip3)/3;
        if (sum_strip3 > avg )
        {
            radius = side/4*1;
        }
        else if (sum_strip2> avg) {
            radius = side/4*2;
        }else if (sum_strip1> avg) {
            radius = side/4*3;
        }else{//default do a long scan
        	radius=side/4*3;
        }
        System.out.println("Total rats : "+rats.length+ " | strip 1 : "+ sum_strip1 + " | strip 2 : "+ sum_strip2 + " | remaining "+ sum_rem + " | RADIUS : "+radius);
        return radius;
    }


	// specify location that the player will alternate between
	public void init(int id, int side, long turns,
					 Point[][] pipers, Point[] rats) {
		this.id = id;
		this.side = side;
		int n_pipers = pipers[id].length;
		pos = new Point[n_pipers][8];
		random_pos = new Point[n_pipers];
		pos_index = new int[n_pipers];
		completed_sweep = new Boolean[n_pipers];
		Arrays.fill(completed_sweep, Boolean.FALSE);

        this.grid = create_grid(this.side, rats.length);
        boolean neg_y = id == 2 || id == 3;
        boolean swap = id == 1 || id == 3;
        our_gate = point(0, side * 0.5 * 1, neg_y, swap);
        update_grid_weights(rats, pipers, our_gate);

        // sort the cells in the Cell[] grid in descending order of weight/number_of_rats
        Arrays.sort(this.grid, Collections.reverseOrder());
        piper_to_cell = get_piper_to_cell(pipers[id].length);
        for (int p=0; p<pipers[id].length; p++) {
            random_pos[p] = piper_to_cell.get(p);
        }
        
        if (isSparse(rats.length, side))
                sparse_flag = true;
		for (int p = 0; p != n_pipers; ++p) {
			// spread out at the door level
			double door = 0.0;
			if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;
			// pick coordinate based on where the player is
			
            our_gate = point(door, side * 0.5, neg_y, swap);
			Point before_gate = point(door, side * 0.5 * .85, neg_y, swap);
			Point inside_gate = point(door, side * 0.5 * 1.2, neg_y, swap);// first and third position is at the door
            Point[] boundaries = new Point[3];
            boundaries[0] = point(side * 0.5 * 1, side * 0.5 * 1, neg_y, swap); // At the door
            boundaries[1] = point(side * 0.5 * 0.5, side * 0.5 * 0.5, neg_y, swap); // Between door and center
            boundaries[2] = point(0, 0, neg_y, swap); // At the center of the grid
            double distance = getSweepRadius(rats, boundaries, id);
             
            //fixed new for getSweepRadius2()
			/*
            Point[] boundaries2 = new Point[3];
            boundaries2[0] = point(side * 0.5 * 1, side * 0.5 * 1, neg_y, swap); // At the door
            boundaries2[1] = point(side * 0.5 * 0.5, side * 0.5 * 0.5, neg_y, swap); // Between door and center
            boundaries2[2] = point(0, 0, neg_y, swap); // At the center of the grid
             double distance = getSweepRadius(rats, boundaries2, id);
            */
           
			double theta = Math.toRadians(p * 90.0 / (n_pipers - 1) + 45);
            pos[p][0] = point(door, side * 0.5, neg_y, swap);
            System.out.println("Init pos index 0: " + pos[p][0].x + ", " + pos[p][0].y);
			pos[p][1] = point(distance * Math.cos(theta), (side/2) + (-1) * distance * Math.sin(theta), neg_y, swap);
            System.out.println("Init pos index 1: " + pos[p][1].x + ", " + pos[p][1].y);
			pos[p][2] = before_gate;
			pos[p][3] = inside_gate;
			pos[p][4] = before_gate;
			// sixth position is chosen randomly in the rat moving areaons;
			pos[p][5] = null;

			// seventh and eighth positions are outside the rat moving area
			pos[p][6] = before_gate;
			pos[p][7] = inside_gate;

			// start with first position
			pos_index[p] = 0;
			dst_no = 0;
		}
	}

    public Cell[] create_grid(int side, int number_of_rats) {
    /*
     Returns a Cell[] array of length = number of cells = side/20 * side/20
     */
        int cell_side;

        if(isSparse(number_of_rats, side)) {
            cell_side = side/20;
        }
        else {
            cell_side = side/5;
        }

        int dim = 0;
        if (side % cell_side == 0)
            dim = side/cell_side;
        float half = side/2;
        Cell[] grid = new Cell[dim*dim];
        
        for(int i=0; i < dim; i++) {
            for(int j=0; j < dim; j++) {
                Cell cell = new Cell(
                                cell_side,
                                 new Point(  // X, Y - center
                                           (i + 0.5) * cell_side - half,
                                           (j + 0.5) * cell_side - half
                                           ));

                grid[(i * dim) + j] = cell;
            }
        }
        
        Cell.counter = 0;
        return grid;
    }
    
    public void display_grid() {
        for (int i=0; i < this.grid.length; i++) {
            this.grid[i].display_cell();
        }
    }
    
    public Cell find_cell(Point rat) {
        for (int i=0; i<this.grid.length; i++) {
            Cell cell = this.grid[i];
            double x1 = cell.center.x - cell.side/2;
            double x2 = cell.center.x + cell.side/2;
            double y1 = cell.center.y + cell.side/2;
            double y2 = cell.center.y - cell.side/2;

            if (rat.x >= x1 && rat.x <= x2 && rat.y >= y2 && rat.y <= y1) {
                return cell;
            }
        }
        return null;
    }
    
    public void update_grid_weights(Point[] rats, Point[][] pipers, Point our_gate) {
        for (int i=0; i < this.grid.length; i++) {
            this.grid[i].weight = 0;
        }
        Point[] our_pipers = pipers[id];

        for (Point rat: rats) {
            Cell cell = find_cell(rat);
            if (cell != null) {
                cell.weight = cell.weight + 1;

                // for (Point piper: our_pipers) {
                //     if (Utils.distance(piper, rat) <= 30 && Utils.distance(rat, our_gate) > side/10)
                //         cell.weight = cell.weight + (cell.weight/5);
                // }
            }

        }
    }
    
    public Map<Integer, Point> get_piper_to_cell(int remaining_pipers) {
        Cell[] grid_copy = Arrays.copyOf(grid, grid.length);
        Map<Integer, Point> piper_to_cell = new HashMap<Integer, Point>();
        List<Integer> all_pipers = new ArrayList<Integer>();
        int i;
        int cells_to_consider;
        int sum;
        int avg;
        int n_p_to_i;
        int piper;
        List<Cell> non_zero_cells = new ArrayList<Cell>();
        Iterator<Cell> iter_list;
        Iterator<Integer> iter;

        for (i=0; i<remaining_pipers; i++) {
            all_pipers.add(i);
        }

        for (int k=0; k < grid_copy.length; k++) {
            non_zero_cells.add(grid_copy[k]);
        }
        
        cells_to_consider = remaining_pipers;
        while (remaining_pipers > 0) {
            int prev_length = non_zero_cells.size();
            if (prev_length > cells_to_consider) {
                non_zero_cells = non_zero_cells.subList(0, cells_to_consider);
            }      
            sum = 0;

            iter_list = non_zero_cells.iterator();
            for (i=0; i<cells_to_consider; i++) {
                Cell next_item = iter_list.next();
                 if (next_item.weight != 0)
                    sum += next_item.weight;
                else
                    iter_list.remove();
            }
            cells_to_consider = non_zero_cells.size();
            if (cells_to_consider == 0)
                break;
            avg = sum/cells_to_consider;

            i = 0;
            iter_list = non_zero_cells.iterator();
            Cell this_cell;
            while(i < cells_to_consider) {
                if (iter_list.hasNext())
                    this_cell = iter_list.next();
                else 
                    break;
                
                n_p_to_i = this_cell.weight/avg;
                
                iter = all_pipers.iterator();
                for (int j=0; j<n_p_to_i; j++) {
                    if (iter.hasNext())
                    {
                        piper = iter.next();
                        piper_to_cell.put(piper, this_cell.center);
                        iter.remove();
                        remaining_pipers -= 1;
                    }
                }
                this_cell.weight = this_cell.weight % avg;
                if (this_cell.weight == 0)
                    iter_list.remove();
                cells_to_consider = non_zero_cells.size();
                if (cells_to_consider == 0)
                    break;
                i++;
            }
        }

        while (remaining_pipers > 0) {
            iter = all_pipers.iterator();
            if (iter.hasNext()) {
                    piper = iter.next();
                    piper_to_cell.put(piper, grid_copy[0].center);
                    iter.remove();
                    remaining_pipers -= 1;
            }
        }     
        return piper_to_cell; 
    }

    // Yields the number of rats within range
    static int num_captured_rats(Point piper, Point[] rats) {
        int num = 0;
        for (Point rat : rats)
            num += Utils.distance(piper, rat) <= 10 ? 1 : 0;
        return num;
    }

    static boolean isSparse(double ratsLength, double side) {
        double density = ratsLength / (side * side);
        if (density <= density_threshold) 
            return true;
        else 
            return false;
    }

	// return next locations on last argument
	public void play(Point[][] pipers, boolean[][] pipers_played,
					 Point[] rats, Move[] moves) {
        tick++;
        try {
            if (tick % 80 == 0) {
                grid = create_grid(side, rats.length);
                update_grid_weights(rats, pipers, our_gate);
                // sort the cells in the Cell[] grid in descending order of weight/number_of_rats
                Arrays.sort(this.grid, Collections.reverseOrder());
                piper_to_cell = get_piper_to_cell(pipers[id].length);
            }
            
            //p : is the index of piper for current player
            for (int p = 0; p != pipers[id].length; ++p) {
                System.out.println(pos_index[p] + "");
                Point src = pipers[id][p];
                System.out.println("src: " + src.x + ", " + src.y);
                Point dst = pos[p][pos_index[p]];
                

                if ((sparse_flag || ((!sparse_flag) && completed_sweep[p])) && (pos_index[p] == 1 ))
                {
                    pos_index[p] = 4;
                }
                // if null then get random position
                if (dst == null) {
                	dst = random_pos[p];
                }
               
                // if position is reached
                if (Math.abs(src.x - dst.x) < 0.000001 &&
                    Math.abs(src.y - dst.y) < 0.000001) {
                    // discard random position
                    if (dst == random_pos[p]) random_pos[p] = null;
                    // get next position
                    if (++pos_index[p] == pos[p].length){
                        pos_index[p] = 0;
                        completed_sweep[p] = true;
                    }
                    dst = pos[p][pos_index[p]];
                    // generate a new position if random
                    if (dst == null || pos_index[p] == 5) {
                        random_pos[p] = dst = piper_to_cell.get(p);
                    }
                }
                System.out.println("dst: " + dst.x + ", " + dst.y);
                if (pos_index[p] == 6 && num_captured_rats(pipers[id][p], rats) == 0)
                    pos_index[p] = 5;
                if ((pos_index[p] == 3 || pos_index[p] == 7) && num_captured_rats(pipers[id][p], rats) == 0)
                    pos_index[p] = 4;
                if (pos_index[p] == 5 ) {
                    random_pos[p] = dst = piper_to_cell.get(p);
                }

                // get move towards position
                moves[p] = move(src, dst, (pos_index[p] > 1 && pos_index[p] < 4) || (pos_index[p] > 5));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
	}
}