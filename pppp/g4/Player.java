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
    private Cell[] grid = null;
    private Boolean sparse_flag = false;
    int tick = 0;
    Point our_gate = null;
    Point near_gate = null;
    Point[] box_boundaries = new Point[2];
    Boolean[] isBoundaryRat = null; // flag to set for playing music when rat in boundary
    Boolean[] completed_sweep = null; // flag to check if sweep is over
    Map<Integer, Point> piper_to_cell = new HashMap<Integer, Point>(); // stores piper to cell assignment
    Map<Point, Set<Integer>> cell_to_piper = new HashMap<Point, Set<Integer>>(); // stores cell to piper assignment

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

    /* Returns true if the piper is close to home */
    public Boolean piper_close_to_home(Point our_piper) {

        // Current value for near_gate = side/6 | Set in init();
        if (id == 0 || id == 2) { 
            //Only consider y axis boundaries
            if ((Math.min(our_gate.y, near_gate.y) < our_piper.y) 
                && (our_piper.y < Math.max(our_gate.y, near_gate.y))) {
                //Piper is now close to home
                return Boolean.TRUE;
            }
        }
        else {
            //Only consider x axis boundaries
            if ((Math.min(our_gate.x, near_gate.x) < our_piper.x) 
                && (our_piper.x < Math.max(our_gate.x, near_gate.x))) {
                //Piper is now close to home
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /* Returns the fixed 6 points where we send all pipers proportionally */
    public Point[] get_sweep_coordinates(int side)
    {
        Point[] sweep_coord = new Point[6];

        sweep_coord[4] = new Point(side * -0.5 * 0.5, side * 0.5 * 0.35);
        sweep_coord[1] = new Point(side * 0.5 * 0.5, side * 0.5 * 0.35);
        sweep_coord[2] = new Point(side * -0.5 * 0.8, side * 0.5 * 0.9);
        sweep_coord[0] = new Point(side * -0.5 * 0.8, side * 0.5 * 0.3);
        sweep_coord[3] = new Point(side * 0.5 * 0.8, side * 0.5 * 0.9);
        sweep_coord[5] = new Point(side * 0.5 * 0.8, side * 0.5 * 0.3);
        return sweep_coord;
    }

    /* Returns the fixed 4 points where all our pipers meet */
    public Point[] get_sweep_return_coordinates(int side)
    {
        Point[] sweep_coord = new Point[4];
        sweep_coord[0] = new Point(side * -0.5 * 0.5, side * 0.5 * 0.6);
        sweep_coord[1] = new Point(side * 0.5 * 0.5, side * 0.5 * 0.6);
        sweep_coord[2] = new Point(side * -0.5 * 0.6, side * 0.5 * 0.6);
        sweep_coord[3] = new Point(side * 0.5 * 0.6, side * 0.5 * 0.6);
        return sweep_coord;
    }

    // specify location that the player will alternate between
    public void init(int id, int side, long turns,
                     Point[][] pipers, Point[] rats) {
        this.id = id;
        this.side = side;
        int n_pipers = pipers[id].length;
        pos = new Point[n_pipers][9];
        random_pos = new Point[n_pipers];
        pos_index = new int[n_pipers];

        completed_sweep = new Boolean[n_pipers];
        Arrays.fill(completed_sweep, Boolean.FALSE);

        isBoundaryRat = new Boolean[n_pipers];
        Arrays.fill(isBoundaryRat, Boolean.FALSE);

        boolean neg_y = id == 2 || id == 3;
        boolean swap = id == 1 || id == 3;

        // pick coordinate based on where the player is
        our_gate = point(0.0, side * 0.5 * 1, neg_y, swap);
        near_gate = point(0.0, side/6, neg_y, swap);

        this.grid = create_grid(side, rats.length);
        update_grid_weights(rats, pipers, our_gate);
        Arrays.sort(this.grid, Collections.reverseOrder());
        piper_to_cell = get_piper_to_cell(pipers);
        for (int p=0; p < pipers[id].length; p++) {
            random_pos[p] = piper_to_cell.get(p);
        }
        
        //Sets boundaries to get 'dynamic' sweep radius
        Point[] boundaries = new Point[3];
        boundaries[0] = point(side * 0.5 * 1, side * 0.5 * 1, neg_y, swap); // At the door
        boundaries[1] = point(side * 0.5 * 0.5, side * 0.5 * 0.5, neg_y, swap); // Between door and center
        boundaries[2] = point(0, 0, neg_y, swap); // At the center of the grid

        // New box_boundaries based on gate_no for piper to change path slightly to pick up rats near its gate
        box_boundaries[0] = point(side * 0.5 * -0.5, side * 0.5 * 0.75, neg_y, swap);
        box_boundaries[1] = point(side * 0.5 * 0.5, side * 0.5, neg_y, swap);
        
        if (Utils.isSparse(rats.length, side, (50/(side * side)))) {
            // sparse_flag is for sweeping. 
            sparse_flag = true;
        }

        Point[] all_points = new Point[6];
        Point[] all_return_points = new Point[4];

        all_points = get_sweep_coordinates(side);
        all_return_points = get_sweep_return_coordinates(side);

        int sum_of_together_pipers = pipers[id].length - 1;
        int max_id = sum_of_together_pipers/2;

        int[] assignment = new int[n_pipers];

        for (int p = 0; p != n_pipers; ++p) {

            int fake_p = 0;
            if (p >= max_id ) {
                fake_p = sum_of_together_pipers - p;
                assignment[p] = assignment[fake_p];
            }
            else {
                assignment[p] = p % 6;
            }

            if (fake_p < 0) 
                fake_p = 0;

            double door = 0.0;
            if (n_pipers != 1) door = p * 1.8 / (n_pipers - 1) - 0.9;

            // pick coordinate based on where the player is
            Point before_gate = point(door, side * 0.5 * .85, neg_y, swap);
            Point inside_gate = point(door, side * 0.5 * 1.2, neg_y, swap);// first and third position is at the door
            double theta = Math.toRadians(p * 90.0 / (n_pipers - 1) + 45);
            
            pos[p][0] = point(0, side * 0.5, neg_y, swap);

            // go to sweep coordinates as per your assignment
            pos[p][1] = (n_pipers==1 ? null: point(all_points[assignment[p]].x, all_points[assignment[p]].y, neg_y, swap));

            // meet at sweep-return coordinates as per the following assignment
            if ( (p % 6) == 0 ) 
                pos[p][2] = all_return_points[0];
            else if ( (p % 6) == 1 ) 
                pos[p][2] = all_return_points[1];
            else if ( (p % 6) == 2 || (p % 6) == 4 ) 
                pos[p][2] = all_return_points[2];
            else if ( (p % 6) == 3 || (p % 6) == 5 ) 
                pos[p][2] = all_return_points[3];

            pos[p][3] = before_gate;
            pos[p][4] = inside_gate;
            pos[p][5] = before_gate;
            pos[p][6] = null;

            // eight and ninth positions are outside the rat moving area
            pos[p][7] = before_gate;
            pos[p][8] = inside_gate;

            // start with first position
            pos_index[p] = 0;
            dst_no = 0;
        }
    }

    /*
     Returns a Cell[] array of length = number of cells = side/20 * side/20
    */
    public Cell[] create_grid(int side, int number_of_rats) {
    
        int cell_side;

        if(Utils.isSparse(number_of_rats, side, (5/(side*side)))) {
            cell_side = 5;
        }
        else {
            cell_side = side/5;
        }

        int dim = 0;
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
    
    /* Returns the Cell to which a rat belongs */
    public Cell find_cell(Point rat) {
        for (int i=0; i < this.grid.length; i++) {
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

    /* Returns 1, if the rat is uncaptured.
               2, if the rats is captured by an opponent
               3, if the rat is captured by a team member */
    public int isAvailableRat(Point rat, Point[][] pipers){
        for (int i=0; i<4; i++) { 
            for(int j=0; j<pipers[i].length; j++) {
                if (Utils.distance(pipers[i][j], rat) <= 10) {
                    if (i == id) 
                        // status 3 means not available and with teammate
                        return 3; 
                    else 
                        // status 2 means not available and with opponent
                        return 2;
                }
            }
        }
        // status 1 means rat is available & not with team mate or opponent
        return 1;
    }
    
    /* Updates grid weights based on various factors. See below for details. */
    public void update_grid_weights(Point[] rats, Point[][] pipers, Point our_gate) {
            for (int i=0; i < this.grid.length; i++) {
                this.grid[i].weight = 0;
            }
            Point[] our_pipers = pipers[id];

            for (Point rat: rats) {
                Cell cell = find_cell(rat);
                if (cell != null) {
                    // each rat adds a weight of 10 to it's cell
                    cell.weight = cell.weight + 10;

                    int status = isAvailableRat(rat, pipers);
                    if (status == 1) {
                        //rat is available
                        cell.weight = cell.weight + 2;
                    }
                    if (Utils.distance(our_gate, rat) <= side/2 && Utils.distance(rat, our_gate) > side/10) {
                        // rat is within our half of the grid, but not too close to the gate
                        cell.weight = cell.weight + 1;
                    }
                    if (Utils.distance(rat, our_gate) < 12) {
                        // cell is too close to the gate
                        cell.weight = cell.weight - 10;
                    }
                }

            }
        }
    
    /* Returns piper to cell assignment based on grid weights */

    public Map<Integer, Point> get_piper_to_cell(Point[][] pipers ) {
        Cell[] grid_copy = Arrays.copyOf(grid, grid.length);
        Map<Point, Integer> n_pipers_needed = new HashMap<Point, Integer>();
        Set<Integer> unassigned_pipers = new HashSet<Integer>();
        int i;
        int cells_to_consider;
        int sum;
        int avg;
        int n_p_to_i;
        int piper;
        List<Cell> non_zero_cells = new ArrayList<Cell>();
        Iterator<Cell> iter_non_zero_cells;
        Iterator<Integer> iter_unassigned_pipers;
        Map<Point, Set<Integer>> cell_to_piper_copy = new HashMap<Point, Set<Integer>>(cell_to_piper);
        cell_to_piper = new HashMap<Point, Set<Integer>>();
        
        // add all pipers to unassigned list
        for (i = 0; i< pipers[id].length; i++) {
            unassigned_pipers.add(i);
        }
        for (Map.Entry<Integer, Point> entry: piper_to_cell.entrySet()) {
            // if a destination is assigned to a piper, remove it from set of unassigned pipers 
            if (entry.getValue() != null)
                unassigned_pipers.remove(entry.getKey());
        }
        
        // start with considering all pipers
        int remaining_pipers = pipers[id].length;
        
        // consider only those many cells as many pipers are left
        cells_to_consider = remaining_pipers;
        
        // add all cells to non zero cells
        for (int k=0; k < grid_copy.length; k++) {
            non_zero_cells.add(grid_copy[k]);
        }
        
        // until all pipers are assigned to something
        while (remaining_pipers > 0) {
            int prev_length = non_zero_cells.size();
            if (prev_length > cells_to_consider) {
                // if non_zero_cells has more than cells_to_consider elements, pick cells_to_consider number of cells 
                non_zero_cells = non_zero_cells.subList(0, cells_to_consider);
            }
            // else non_zero_cells already has fewer than cells_to_consider cells

            sum = 0;

            iter_non_zero_cells = non_zero_cells.iterator();
            for (i=0; i<cells_to_consider; i++) {
                Cell next_item = iter_non_zero_cells.next();
                // add non zero cell values
                if (next_item.weight != 0)
                    sum += next_item.weight;
                else
                // if weight is zero, remove from non_zero_cells
                    iter_non_zero_cells.remove();
            }
            // update cells_to_consider
            cells_to_consider = non_zero_cells.size();
            if (cells_to_consider == 0)
                break;
            
            avg = sum/cells_to_consider;

            i = 0;
            iter_non_zero_cells = non_zero_cells.iterator();
            Cell this_cell;
            while(i < cells_to_consider) {
                if (iter_non_zero_cells.hasNext())
                    this_cell = iter_non_zero_cells.next();
                else 
                    break;
                // number of pipers to send to i-th cell = n_p_to_i
                n_p_to_i = this_cell.weight/avg;
                
                // update map of how many pipers needed for top destinations
                int previous_assigned = 0;
                if (n_pipers_needed.containsKey(this_cell.center))
                    previous_assigned = n_pipers_needed.get(this_cell.center);
                n_pipers_needed.put(this_cell.center, previous_assigned + n_p_to_i);
                
                // update remaining_pipers
                remaining_pipers -= n_p_to_i;
                
                // update weight and set to remainder
                this_cell.weight = this_cell.weight % avg;

                // if weight becomes 0, remove from non_zero_cells
                if (this_cell.weight == 0)
                    iter_non_zero_cells.remove();
                
                
                i++;
            }
            // look at remaining_piper number of cells. But we might not have enough non_zero_cells, so take the minimum of the two
            cells_to_consider = Math.min(remaining_pipers, non_zero_cells.size());

            // if cells_to_consider is 0, break
            if (cells_to_consider == 0)
                break;
            
            // end while loop here
        }
            
        iter_unassigned_pipers = unassigned_pipers.iterator();
        // maintain previous assignments if destination hasn't changed
        for (Point destination: n_pipers_needed.keySet()) {
            if (cell_to_piper_copy.containsKey(destination)) {
                // if someone was assigned to this destination already, copy the set back this time
                cell_to_piper.put(destination, cell_to_piper_copy.get(destination));
            }
            else {
                // if no one was assigned to this destination, add the destination to the map with an empty set
                cell_to_piper.put(destination, new HashSet<Integer>());
            }
        }
        
        for (Map.Entry<Point, Integer> entry: n_pipers_needed.entrySet()) {
            Point dst = entry.getKey();
            int need = entry.getValue();
            
            if (cell_to_piper.get(dst).size() < need) {
                // if the given cell is assigned lesser than what it needs, find next free piper and assign it to this cell
                i = 0;
                int previously_assigned =  cell_to_piper.get(dst).size();
                while (i < need - previously_assigned) {
                    if (iter_unassigned_pipers.hasNext())
                    {
                        piper = iter_unassigned_pipers.next();
                        cell_to_piper.get(dst).add(piper);
                        iter_unassigned_pipers.remove();
                        piper_to_cell.put(piper, dst);
                    }
                    i++;
                }
            }
        }
        
        iter_unassigned_pipers = unassigned_pipers.iterator();
        while (remaining_pipers > 0 && iter_unassigned_pipers.hasNext()) {
            
            piper = iter_unassigned_pipers.next();
            piper_to_cell.put(piper, grid_copy[0].center);
            iter_unassigned_pipers.remove();
            remaining_pipers -= 1;
        }   
         
        return piper_to_cell; 
    }

    // return next locations on last argument
    public void play(Point[][] pipers, boolean[][] pipers_played,
                     Point[] rats, Move[] moves) {
        grid = create_grid(side, rats.length);
        update_grid_weights(rats, pipers, our_gate);
        Arrays.sort(this.grid, Collections.reverseOrder());
        piper_to_cell = get_piper_to_cell(pipers);

        for (int p = 0; p != pipers[id].length; ++p) {

            Point src = pipers[id][p];
            Point dst = pos[p][pos_index[p]];

            if ((sparse_flag || ((!sparse_flag) && completed_sweep[p])) && (pos_index[p] == 1 )) {
                pos_index[p] = 5;
            }

            if (dst == null) {
                dst = random_pos[p];
            }

            if ((Math.abs(src.x - dst.x) < 0.000001 &&
                Math.abs(src.y - dst.y) < 0.000001) || (pos_index[p] == 6 && Utils.getRatsCountOnDst( rats,   dst, 10)==0 )) 
            {
                // discard random position
                if (dst == random_pos[p]) random_pos[p] = null;
                // get next position
                if (++pos_index[p] == pos[p].length){
                    pos_index[p] = 0;
                    completed_sweep[p] = true;
                }
                dst = pos[p][pos_index[p]];

                // generate a new position if random
                if (dst == null || pos_index[p] == 6) {
                    random_pos[p] = dst = piper_to_cell.get(p);
                }
            }

            if ((pos_index[p] == 7 ) && (Utils.num_captured_rats(pipers[id][p], rats) == 0)) {
                pos_index[p] = 6;
            }
            if ((pos_index[p] == 7) 
                && (Utils.num_captured_rats(pipers[id][p], rats) >= 1) 
                && (Utils.get_closest_rat(rats, box_boundaries, pipers[id][p]) != null) 
                && (!isBoundaryRat[p]) && (piper_close_to_home(pipers[id][p])))
            {      
                pos_index[p] = 6;
                random_pos[p] = dst = Utils.get_closest_rat(rats, box_boundaries, pipers[id][p]);
                isBoundaryRat[p] = Boolean.TRUE;
            }

            if ((pos_index[p] == 4 || pos_index[p] == 8) && Utils.num_captured_rats(pipers[id][p], rats) == 0)
                pos_index[p] = 5;
            if ((pos_index[p] == 6 ) && (!isBoundaryRat[p])){
                // just got free to do something
                // reassign piper to null destination first - no longer assigned to previous cell 
                // (because we're using non-null values in this map to compute set of unassigned pipers)
                piper_to_cell.put(p, null);

                // update grid now
                grid = create_grid(side, rats.length);
                update_grid_weights(rats, pipers, our_gate);
                Arrays.sort(this.grid, Collections.reverseOrder());
                piper_to_cell = get_piper_to_cell(pipers);

                if (random_pos[p] == null)
                    random_pos[p] = dst = piper_to_cell.get(p);
            }

            // get move towards position
            boolean play = (pos_index[p] > 1 && pos_index[p] < 5) || (pos_index[p] > 6) || (isBoundaryRat[p] && (Utils.num_captured_rats(pipers[id][p], rats) >= 1));
            moves[p] = move(src, dst, play);
        }
        
        
    }
}