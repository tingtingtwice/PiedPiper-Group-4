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
    Boolean[] reachedFirstPoint=null;
    private Cell[] grid = null;
    private static double density_threshold = 0.005;
    private Boolean sparse_flag = false;
    private Boolean enableClustering=false;
 
    int[] whichCurrentCluster=null;
    double[] maxDistanceInCurrentCluster=null;
    private int[] posIndexInCurrentCluster = null;
    int stepNo=1;
    int tick = 0;
    Point our_gate = null;
    private List<Cluster> clusters=new ArrayList<Cluster>();
    int nearbyRatScanRadius=10; //used in function checking if there are nearby rats
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
    
    @Override
    public void init(int id, int side, long turns, Point[][] pipers, Point[] rats) {
        int n_pipers = pipers[id].length;
        if((rats.length==101 || rats.length==500) && (side==100 || side==200) && n_pipers==1 ){
            enableClustering=true;
            initForClusteredRun(  id,   side,   turns,
                     pipers,   rats);
        }else{
            enableClustering=false;
            initForNormalRun(  id,   side,   turns,
                      pipers,  rats) ;
        }
    }
// specify location that the player will alternate between
    //    init(g, inner_side, turn_limit, pipers_copy, rats_copy)
    public void initForClusteredRun(int id, int side, long turns,
                     Point[][] pipers, Point[] rats) {
        this.id = id;// Player Id
        this.side = side; 
        int n_pipers = pipers[id].length;
        //reset cluster positions for each piper
        whichCurrentCluster=new int[n_pipers];
        Arrays.fill(whichCurrentCluster, -1);
        
        maxDistanceInCurrentCluster=new double[n_pipers];
        Arrays.fill(maxDistanceInCurrentCluster, (int) (side * .8));
        
        posIndexInCurrentCluster = new int[n_pipers];
        Arrays.fill(posIndexInCurrentCluster, 0);
        pos = new Point[n_pipers][8];
        random_pos = new Point[n_pipers];
        pos_index = new int[n_pipers];
        completed_sweep = new Boolean[n_pipers];
        reachedFirstPoint= new Boolean[n_pipers];
        Arrays.fill(completed_sweep, Boolean.FALSE);
        Arrays.fill(reachedFirstPoint, Boolean.FALSE);
        this.grid = create_grid(this.side, rats.length);
        boolean neg_y = id == 2 || id == 3; //2(south) || 3(west)
        boolean swap = id == 1 || id == 3; //1(EAST) || 3 (WEST) we calculate point for NORTH and want to swap / negate Y based on position of player
        our_gate = point(0, side * 0.5, neg_y, swap);
        update_grid_weights(rats, pipers, our_gate);

        // sort the cells in the Cell[] grid in descending order of weight/number_of_rats
        Arrays.sort(this.grid, Collections.reverseOrder());
 
        for (int p=0; p<pipers[id].length; p++) {
            random_pos[p] = piper_to_cell.get(p); //random_pos == next most imp cell
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
    // specify location that the player will alternate between
    public void initForNormalRun(int id, int side, long turns,
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
        
        if (Utils.isSparse(rats.length, side, (50.0/(side * side)))) {
            // sparse_flag is for sweeping. 
            sparse_flag = true;
        }

        Point[] all_points = new Point[6];
        Point[] all_return_points = new Point[4];

        all_points = get_sweep_coordinates(side);
        all_return_points = get_sweep_return_coordinates(side);

        int sum_of_together_pipers = pipers[id].length - 1;
        int max_id = sum_of_together_pipers/2;
        if (max_id < 6)
            max_id = 6;
    
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
                pos[p][2] = point(all_return_points[0].x, all_return_points[0].y, neg_y, swap);
            else if ( (p % 6) == 1 ) 
                pos[p][2] = point(all_return_points[1].x, all_return_points[1].y, neg_y, swap);
            else if ( (p % 6) == 2 || (p % 6) == 4 ) 
                pos[p][2] = point(all_return_points[2].x, all_return_points[2].y, neg_y, swap);
            else if ( (p % 6) == 3 || (p % 6) == 5 ) 
                pos[p][2] = point(all_return_points[3].x, all_return_points[3].y, neg_y, swap);

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

        if(Utils.isSparse(number_of_rats, side, (5.0/(side*side)))) {
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
                        cell.weight = cell.weight + 3;
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
        if(enableClustering==true){
             playUsingClusteredPaths( pipers,   pipers_played, rats,  moves);
        }else{
             playForNomalRun( pipers,   pipers_played, rats,  moves);
        }
    }
    // return next locations on last argument
    public void playForNomalRun(Point[][] pipers, boolean[][] pipers_played,
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
                Math.abs(src.y - dst.y) < 0.000001) ) 
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

            if ((pos_index[p] == 6 && Utils.getRatsCountOnDst( rats,   dst, 10)==0 )) {
                piper_to_cell = get_piper_to_cell(pipers);
                random_pos[p] = dst = piper_to_cell.get(p);            
            }

            if ((pos_index[p] == 7 ) && (Utils.num_captured_rats(pipers[id][p], rats) == 0)) {
                pos_index[p] = 6;
            }

            if ((pos_index[p] == 4 || pos_index[p] == 8) && Utils.num_captured_rats(pipers[id][p], rats) == 0)
                pos_index[p] = 5;
            if ((pos_index[p] == 6 )){
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
            boolean play = (pos_index[p] > 1 && pos_index[p] < 5) || (pos_index[p] > 6);
            moves[p] = move(src, dst, play);
        }
        
    }
    private void updateClusters(Point[] rats){
        //KMeans kmeans = new KMeans();
        int clusterCount = 3;
        int minX=-50;
        int maxX=50;
        KMeans kmeans = new KMeans(clusterCount, minX, maxX, new ClusterPoint(our_gate.x,our_gate.y));
        
        //Set Random Centroids
        List<ClusterPoint> centerPoints=new ArrayList<ClusterPoint>();
        centerPoints.add(new ClusterPoint(side/4,side/4));
        centerPoints.add(new ClusterPoint(-1 * side/4,side/4));
        centerPoints.add(new ClusterPoint(0,side/4));
        
        kmeans.init(rats, centerPoints);
        kmeans.calculate();
        kmeans.printClusterSizes(); 
        this.clusters=kmeans.sortClustersAndUpdateShortestPathsInCluster();

        kmeans.plotClusters();
        kmeans.printClusterSizes(); 
    }
    // return next locations on last argument
        public void playUsingClusteredPaths(Point[][] pipers, boolean[][] pipers_played,
                         Point[] rats, Move[] moves) {
            tick++;
            try {
                if (clusters.size()==0 || tick % (side * 2 * 0.6) == 0) {
                    updateClusters(rats);
                }
                
                //p : is the index of piper for current player
                for (int p = 0; p != pipers[id].length; ++p) {
                    //increment the Position to 6 when already captured 30 rats
                    Point src = pipers[id][p];
                    Point dst =null;
                    System.out.println("src: " + src.x + ", " + src.y);
                    int capturedRats= Utils.num_captured_rats(pipers[id][p], rats);
                
                        if(pos_index[p]==0 && pos[p][pos_index[p]]!=null && Utils.reachedDst(src,pos[p][pos_index[p]])){
                            pos_index[p]=2;
                        }else if(pos_index[p]==2 && pos[p][pos_index[p]]!=null  && Utils.reachedDst(src,pos[p][pos_index[p]])){
                            pos_index[p]=5;
                            stepNo=1;
                            
                        }else if(pos_index[p]==5 && pos[p][pos_index[p]]!=null && Utils.reachedDst(src,pos[p][pos_index[p]]) && (capturedRats >20 || stepNo==3)){
                            pos_index[p]=6;
                        }else if(pos_index[p]==5 && pos[p][pos_index[p]]!=null && Utils.reachedDst(src,pos[p][pos_index[p]]) && reachedFirstPoint[p]==true )
                        {
                            stepNo+=1;
                        }
                        else if (pos_index[p]==6 && pos[p][pos_index[p]]!=null && Utils.reachedDst(src,pos[p][pos_index[p]])){
                            pos_index[p]=7;
                        }else if (pos_index[p]==7 && pos[p][pos_index[p]]!=null && Utils.reachedDst(src,pos[p][pos_index[p]])){
                            pos_index[p]=0;
                        }
                    /*}else if(Utils.num_captured_rats(pipers[id][p], rats)<1 ){
                        reachedFirstPoint[p]=false;
                    }*/
                    // if inside the GATE Or first run, update clusterss
                    if(pos_index[p] ==0 ){
                        //if reached point then move index to 2
                        if (Utils.reachedDst(src,pos[p][pos_index[p]])){
                            pos_index[p]=2;
                        }
                        reachedFirstPoint[p]=false;
                        dst= pos[p][pos_index[p]];
                        moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                        continue;
                    } else if(pos_index[p] ==2 ){
                        //if reached point then move index to 5
                        if (Utils.reachedDst(src,pos[p][pos_index[p]])){
                            pos_index[p]=5;
                        }
                        reachedFirstPoint[p]=false;
                        dst= pos[p][pos_index[p]];
                        moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                        continue;
                    }                   
                    else if(pos_index[p] ==5){
                         
                        //boolean clusterUnknown=whichCurrentCluster[p];                                && Utils.ratsOnPath( rats , clusters.get(whichCurrentCluster[p]).getPoints(), nearbyRatScanRadius,posIndexInCurrentCluster[p])>0
                        if(pos[p][pos_index[p]]!=null && Utils.reachedDst(src,pos[p][pos_index[p]]) && reachedFirstPoint[p]==true){
                            //generate next Point and move to it
                            //find Next P5 point
                            dst=findNextP5Point(rats,pipers, p);
                            moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                            continue;
                        }
                        if(reachedFirstPoint[p] 
                                && Utils.num_captured_rats( pipers[id][p], rats) >0
                                && Utils.reachedDst(src,pos[p][pos_index[p]])  /* reached DST then reset ?*/){
                            //find Next P5 point
                            dst=null;
                            //until we find the correct DST
                            int iteration=1;
                            while((dst==null ||  Utils.getRatsCountOnDstForClustered( rats, dst,nearbyRatScanRadius) ==0) && iteration<500){
                                DenseCell d=Utils.getInitialConfigForMaxRatCluster(rats, clusters, whichCurrentCluster[p], our_gate, 
                                        ((dst==null || whichCurrentCluster[p]==-1  )? ((isSparse(rats.length, side)  && reachedFirstPoint[p]==false)?side : side*.4 ): maxDistanceInCurrentCluster[p] *.6)) ;//NEED TO capture the most dense cluster in the cluster as start point.
                                if(d==null){
                                    if(Utils.num_captured_rats( pipers[id][p], rats) ==0){
                                        reachedFirstPoint[p]=false;
                                    }
                                    whichCurrentCluster[p]=-1;
                                    dst=pos[p][6];//next call will reset its position
                                    break;
                                }else{
                                    maxDistanceInCurrentCluster[p]=d.distanceFromGate;
                                    posIndexInCurrentCluster[p]=d.pointIndexInCluster;  
                                    pos[p][pos_index[p]]=dst=clusters.get(whichCurrentCluster[p]).getPoints().get(posIndexInCurrentCluster[p]);
                                }
                                iteration+=1;
                            }
                            if(iteration>=500 || dst==null){
                                System.out.println("ERRORRRRRRRRR WHY ");
                                pos_index[p]=6;
                                dst=pos[p][pos_index[p]];
                                moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                                continue;
                            }
                            if(capturedRats<1 &&  Utils.getRatsCountOnDstForClustered( rats, src,nearbyRatScanRadius) ==0){
                                reachedFirstPoint[p]=false;
                            }else if(capturedRats>30){
                                reachedFirstPoint[p]=true;
                                pos_index[p]=6;
                                dst=pos[p][pos_index[p]];
                            }
                            moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                            continue;
                            
                        }
                        else if(whichCurrentCluster[p]==-1  ||
                                Utils.ratsOnPath( rats , clusters.get(whichCurrentCluster[p]).getPoints(), nearbyRatScanRadius,posIndexInCurrentCluster[p]) ==0 ){
                            //TODO: need to handle multiple pipers logic here
                            updateClusters(rats);
                            whichCurrentCluster[p]=KMeans.findMaxClusterIndex(clusters);
                            dst=null;
                            //until we find the correct DST
                            int iteration=1;
                            while(iteration < 500 && (dst==null ||  Utils.getRatsCountOnDstForClustered( rats, dst,nearbyRatScanRadius) ==0)){
                                DenseCell d=Utils.getInitialConfigForMaxRatCluster(rats, clusters, whichCurrentCluster[p], our_gate, 
                                        ((dst==null || whichCurrentCluster[p]==-1  )? ((isSparse(rats.length, side) && reachedFirstPoint[p]==false)?side : side*.4 ): maxDistanceInCurrentCluster[p] *.6)) ;//NEED TO capture the most dense cluster in the cluster as start point.
                                if(d==null){
                                    if(Utils.num_captured_rats( pipers[id][p], rats) ==0){
                                        reachedFirstPoint[p]=false;
                                    }
                                    whichCurrentCluster[p]=-1;
                                    dst=pos[p][6];//next call will reset its position
                                    break;
                                }else{
                                    maxDistanceInCurrentCluster[p]=d.distanceFromGate;
                                    posIndexInCurrentCluster[p]=d.pointIndexInCluster;  
                                    pos[p][pos_index[p]]=dst=clusters.get(whichCurrentCluster[p]).getPoints().get(posIndexInCurrentCluster[p]);
                                }
                                iteration+=1;
                            }
                            if(iteration>=500 || dst==null){
                                System.out.println("ERRORRRRRRRRR WHY ");
                                pos_index[p]=6;
                                dst=pos[p][pos_index[p]];
                                moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                                continue;
                            }
                            if(capturedRats<1 &&  Utils.getRatsCountOnDstForClustered( rats, src,nearbyRatScanRadius) ==0){
                                reachedFirstPoint[p]=false;
                            }else if(capturedRats>30){
                                reachedFirstPoint[p]=true;
                                pos_index[p]=6;
                                dst=pos[p][pos_index[p]];
                            }
                            moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                            continue;
                        }else if(//playing music and nothing on path and nothing on dst.. then refind next most denseCell
                                (
                                reachedFirstPoint[p] && 
                                Utils.ratsOnPath( rats , clusters.get(whichCurrentCluster[p]).getPoints(), nearbyRatScanRadius,posIndexInCurrentCluster[p]) ==0 )||
                                Utils.getRatsCountOnDstForClustered( rats, dst,nearbyRatScanRadius) ==0 ||
                                         pos[p][pos_index[p]] ==null ||
                                        Utils.getRatsCountOnDstForClustered( rats, pos[p][pos_index[p]],nearbyRatScanRadius) ==0 ){
                            
                            dst=pos[p][pos_index[p]];
                            //until we find the correct DST
                            int iteration=1;
                            while(iteration < 500 &&(dst==null ||  Utils.getRatsCountOnDstForClustered( rats, dst,nearbyRatScanRadius) ==0)){
                                updateClusters(rats);
                                DenseCell d=Utils.getInitialConfigForMaxRatCluster(rats, clusters, whichCurrentCluster[p], our_gate, 
                                        ((dst==null ||Utils.getRatsCountOnDstForClustered( rats, pos[p][pos_index[p]],nearbyRatScanRadius) ==0 ) ? ((isSparse(rats.length, side) && reachedFirstPoint[p]==false)?side : side*.4 ) : maxDistanceInCurrentCluster[p] *.6)) ;//NEED TO capture the most dense cluster in the cluster as start point.
                                if(d==null){
                                    if(Utils.num_captured_rats( pipers[id][p], rats) ==0){
                                        reachedFirstPoint[p]=false;
                                    }
                                    whichCurrentCluster[p]=-1;
                                    dst=pos[p][6];//next call will reset its position
                                }else{
                                    maxDistanceInCurrentCluster[p]=d.distanceFromGate;
                                    posIndexInCurrentCluster[p]=d.pointIndexInCluster;  
                                    pos[p][pos_index[p]]=dst=clusters.get(whichCurrentCluster[p]).getPoints().get(posIndexInCurrentCluster[p]);
                                }
                                iteration+=1;
                            }
                            if(iteration>=500 || dst==null){
                                System.out.println("ERRORRRRRRRRR WHY ");
                                pos_index[p]=6;
                                dst=pos[p][pos_index[p]];
                                moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                                continue;
                            }
                            if(posIndexInCurrentCluster[p] >= clusters.get(whichCurrentCluster[p]).getPoints().size()){
                                //pos_index[p]=2;       
                                whichCurrentCluster[p]=-2;//for -2 regenerate clusters and get next densest point in current Cluster towards gate 
                                dst=pos[p][6];
                                //dst=pos[p][pos_index[p]];
                            }
                            //reset music if no nearby rats of current source
                            else if(Utils.num_captured_rats( pipers[id][p], rats) >0 ){//come back greedy
                                //find Next P5 point
                                reachedFirstPoint[p]=true;
                                dst=findNextP5Point(rats,pipers, p);
                                moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                                continue;
                                    //pos_index[p]=6;
                                    //dst=pos[p][pos_index[p]];
                            }else{ //nothing Captured yet so goto next cluster..
                                    reachedFirstPoint[p]=false;
                                    whichCurrentCluster[p]=-1;
                                    dst=pos[p][6];//next call will reset its position
                            }
                            if(capturedRats<1 &&  Utils.getRatsCountOnDstForClustered( rats, src,nearbyRatScanRadius) ==0){
                                reachedFirstPoint[p]=false;
                                whichCurrentCluster[p]=-1;
                                dst=pos[p][6];//next call will reset its position
                            }else if(capturedRats>30){
                                reachedFirstPoint[p]=true;
                                pos_index[p]=6;
                                dst=pos[p][pos_index[p]];
                            }
                            moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                            continue;
                        }//if reached point then start music if rats more then 0 nearby.
                        else if (pos[p][pos_index[p]] !=null && Utils.reachedDst(src,pos[p][pos_index[p]]) 
                                && Utils.num_captured_rats( pipers[id][p], rats) >0 ){
                            reachedFirstPoint[p]=true;//move to next point.. ?
                            //until we find the correct DST
                            int iteration=1;
                            while(iteration < 500 && (dst==null ||  Utils.getRatsCountOnDstForClustered( rats, dst,nearbyRatScanRadius) ==0 || Utils.reachedDst(src,pos[p][pos_index[p]]))){
                                DenseCell d=null;
                                if(whichCurrentCluster[p] ==-1){
                                    if(Utils.num_captured_rats( pipers[id][p], rats) ==0){
                                        reachedFirstPoint[p]=false;
                                    }
                                    //whichCurrentCluster[p]=-1;
                                    dst=pos[p][6];//next call will reset its position
                                }else{
                                    updateClusters(rats);
                                    d=Utils.getInitialConfigForMaxRatCluster(rats, clusters, whichCurrentCluster[p], our_gate, 
                                        (dst==null  ? ( (isSparse(rats.length, side)&&  reachedFirstPoint[p]==false)?side : side*.5 ) : maxDistanceInCurrentCluster[p] *.6)) ;//NEED TO capture the most dense cluster in the cluster as start point.
                                }
                                if(d==null){
                                    if(Utils.num_captured_rats( pipers[id][p], rats) ==0){
                                        reachedFirstPoint[p]=false;
                                    }
                                    whichCurrentCluster[p]=-1;
                                    dst=pos[p][6];//next call will reset its position
                                }else{
                                    maxDistanceInCurrentCluster[p]=d.distanceFromGate;
                                    posIndexInCurrentCluster[p]=d.pointIndexInCluster;  
                                    pos[p][pos_index[p]]=dst=clusters.get(whichCurrentCluster[p]).getPoints().get(posIndexInCurrentCluster[p]);
                                }
                                iteration+=1;
                            }
                                if(iteration>=500 || dst==null){
                                    System.out.println("ERRORRRRRRRRR WHY ");
                                    pos_index[p]=6;
                                    dst=pos[p][pos_index[p]];
                                    moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                                    continue;
                                }
                            if(posIndexInCurrentCluster[p] >= clusters.get(whichCurrentCluster[p]).getPoints().size()){
                                pos_index[p]=6;     
                                dst=pos[p][pos_index[p]];
                            }
                            //reset music if no nearby rats of current source
                            else if(Utils.num_captured_rats( pipers[id][p], rats) >0 ){//come back greedy
                                //find Next P5 point
                                reachedFirstPoint[p]=true;
                                dst=findNextP5Point(rats, pipers, p);
                                moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                                continue;
                            }else{ //nothing Captured yet so goto next cluster..
                                    reachedFirstPoint[p]=false;
                                    whichCurrentCluster[p]=-1;
                                    dst=pos[p][6];//next call will reset its position
                            }
                            if(capturedRats<1 &&  Utils.getRatsCountOnDstForClustered( rats, src,nearbyRatScanRadius) ==0){
                                reachedFirstPoint[p]=false;
                                whichCurrentCluster[p]=-1;
                                dst=pos[p][6];//next call will reset its position
                            }else if(capturedRats>30){
                                //find Next P5 point
                                dst=findNextP5Point(rats,pipers, p);
                                moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                                continue;
                            }
                            moves[p] = move(src, dst,  (pos_index[p] >= 5 && reachedFirstPoint[p]));
                            continue;
                        }else{
                            dst=pos[p][pos_index[p]];
                            moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                            continue;
                        }
                    } else if(pos_index[p] ==6){
                        //if unnecessary nove to 6 then goto 5 again
                        if(capturedRats<1 &&  Utils.getRatsCountOnDstForClustered( rats, src,nearbyRatScanRadius) ==0){
                            reachedFirstPoint[p]=false;
                            whichCurrentCluster[p]=-1;
                            pos_index[p]=5;
                            stepNo=1;
                            dst=pos[p][6];//next call will reset its position
                        }
                        //if reached point then move index to 5
                        if (Utils.reachedDst(src,pos[p][pos_index[p]])){
                            pos_index[p]=7;
                        }
                        dst= pos[p][pos_index[p]];
                        moves[p] = move(src, dst, (pos_index[p] >= 5 && reachedFirstPoint[p]));
                        continue;
                    } else if(pos_index[p] ==7){
                        //if reached point then move index to 5
                        if (Utils.reachedDst(src,pos[p][pos_index[p]])){
                            pos_index[p]=0;
                        }
                        dst= pos[p][pos_index[p]];
                        reachedFirstPoint[p]=true;
                        moves[p] = move(src, dst,  (pos_index[p] >= 5 && reachedFirstPoint[p]));
                        
                        continue;
                    } 
                    
                    /*//-----check  case where no rats actually in the path of traversal.. in case.. reset the point and distance and posIndex
                    if(pos_index[p]==5 && 
                            ){
                        whichCurrentCluster[p]=KMeans.findMaxClusterIndex(clusters);
                        maxDistanceInCurrentCluster[p]= (int) (maxDistanceInCurrentCluster[p]*0.7);
                        posIndexInCurrentCluster[p]=KMeans.resetIndex(clusters, whichCurrentCluster[p],maxDistanceInCurrentCluster[p] );
                        if(capturedRats<1){
                            reachedFirstPoint[p]=false;
                        }else if(capturedRats>30){
                            pos_index[p]=(pos_index[p]+1) %pos[p].length;
                        }
                    }*/
     
               
 /*
                    // if position is reached
                    if (Math.abs(src.x - dst.x) < 0.000001 &&
                        Math.abs(src.y - dst.y) < 0.000001) {
                        // discard random position
                        if(pos_index[p] == 5){
                            reachedFirstPoint[p]=true;
                        }else if(pos_index[p] !=5 && pos_index[p]!= 6){
                            
                            reachedFirstPoint[p]=false;
                        }
        
                    
                        // get next position
                        if (pos_index[p] == pos[p].length-1){
                            pos_index[p] = 0;
        
                            reachedFirstPoint[p]=false;
                        }else if(pos_index[p] == 5 && clusters.get(whichCurrentCluster[p]).getPoints().size()> posIndexInCurrentCluster[p] +1){
                            maxDistanceInCurrentCluster[p]=(int) (maxDistanceInCurrentCluster[p]*0.7);//reduce path by half each time
                            posIndexInCurrentCluster[p]=KMeans.resetIndex(clusters, whichCurrentCluster[p],maxDistanceInCurrentCluster[p] );
                            if(posIndexInCurrentCluster[p] >= clusters.get(whichCurrentCluster[p]).getPoints().size()){
                                pos_index[p]=6;                          
                            }else{
                            //posIndexInCurrentCluster[p]=posIndexInCurrentCluster[p]+1;
                                pos[p][pos_index[p]]=clusters.get(whichCurrentCluster[p]).getPoints().get(posIndexInCurrentCluster[p]);
                            }
                        }else{
                            pos_index[p]=(pos_index[p]+1)%pos[p].length;
                        }
                        
                        dst = pos[p][pos_index[p]];
                        // generate a new position if random
                        if (dst == null || pos_index[p] == 5 && clusters.get(whichCurrentCluster[p]).getPoints().size()> posIndexInCurrentCluster[p] +1) { 
                            maxDistanceInCurrentCluster[p]=(int) (maxDistanceInCurrentCluster[p]*0.7);//reduce path by half each time
                            posIndexInCurrentCluster[p]=KMeans.resetIndex(clusters, whichCurrentCluster[p],maxDistanceInCurrentCluster[p] );
                            if(posIndexInCurrentCluster[p] >= clusters.get(whichCurrentCluster[p]).getPoints().size()){
                                pos_index[p]=6;
                                dst = pos[p][pos_index[p]];
                            }else{
                            //posIndexInCurrentCluster[p]=posIndexInCurrentCluster[p]+1;
                                random_pos[p] = dst =pos[p][pos_index[p]]=clusters.get(whichCurrentCluster[p]).getPoints().get(posIndexInCurrentCluster[p]);
                            }
                            
                        }
                    }
                    System.out.println("dst: " + dst.x + ", " + dst.y);
                    if ((pos_index[p] == 3 || pos_index[p] == 7) && Utils.num_captured_rats(pipers[id][p], rats) == 0)
                        pos_index[p] = 4;
                    // get move towards position
                    moves[p] = move(src, dst, (pos_index[p] > 1 && pos_index[p] < 4) || (pos_index[p] >= 5 && reachedFirstPoint[p]));*/
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            
        }
private Point findNextP5Point(Point[] rats,  Point[][] pipers, int p) {
        Point dst=null;
        //until we find the correct DST
        int iteration=1;
        while((dst==null ||  Utils.getRatsCountOnDstForClustered( rats, dst,nearbyRatScanRadius) ==0) && iteration<500){
            DenseCell d=Utils.getInitialConfigForMaxRatCluster(rats, clusters, whichCurrentCluster[p], our_gate, 
                    ((dst==null || whichCurrentCluster[p]==-1  )? ((isSparse(rats.length, side)  && reachedFirstPoint[p]==false)?side : side*.4 ): maxDistanceInCurrentCluster[p] *.6)) ;//NEED TO capture the most dense cluster in the cluster as start point.
            if(d==null){
                if(Utils.num_captured_rats( pipers[id][p], rats) ==0){
                    reachedFirstPoint[p]=false;
                }
                whichCurrentCluster[p]=-1;
                dst=pos[p][6];//next call will reset its position
                break;
            }else{
                maxDistanceInCurrentCluster[p]=d.distanceFromGate;
                posIndexInCurrentCluster[p]=d.pointIndexInCluster;  
                pos[p][pos_index[p]]=dst=clusters.get(whichCurrentCluster[p]).getPoints().get(posIndexInCurrentCluster[p]);
            }
            iteration+=1;
        }
        if(iteration>=500 || dst==null){
            System.out.println("ERRORRRRRRRRR WHY ");
            pos_index[p]=6;
            return pos[p][pos_index[p]];
        }
        return dst;
    }


}