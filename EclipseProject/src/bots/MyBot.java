 package bots;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import pirates.Drone;
import pirates.Location;
import pirates.MapObject;
import pirates.Pirate;
import pirates.PirateBot;
import pirates.PirateGame;

public class MyBot implements PirateBot {
	public static final int k_SAIL_RANGE = 2;
	public static final int k_ATTACK_RANGE = 3;
	public static final int k_SUPPORT_RANGE = 2;
	public static List<PirateOrders> orders = new LinkedList<PirateOrders>();
	
	public void doTurn(PirateGame game) {
		
		game.debug("turn - " + game.getTurn());
		if(game.getTurn() > 2){
			if(orders.size() == 0) for(int i = 0; i < game.getAllMyPirates().size(); i++){
				PirateOrders order = new PirateOrders(i, game.getAllIslands().get(i % game.getAllIslands().size()),
						PirateOrders.PirateOrderType.attack, game);
				orders.add(order);
			}
			for(int i = 0; i < game.getAllMyPirates().size(); i++){
				PirateOrders order = orders.get(i);
				if(order.isComplete()) {
					game.debug("Order complete! id - " + i);
				}
				order.update(game);
			}
		}
	}
	
}
/**
 * Order class for a given pirate.
 * @author Yoav
 */
class PirateOrders{
	private Pirate subject; // The Pirate Subject to the orders
	private boolean completed = false; // Indicates whether the order is complete
	private List<Location> path; // Path of the pirate
	private BiConsumer<PirateGame, PirateOrders> order; // Order body
	private MapObject destination; // Destination object the subject should reach
	private int subjectId; // Id of the subject
	/**
	 * Builds a PirateOrder. 
	 * @param subjectId - the subject id ({@link Pirate}) on whom the order was given. 
	 * @param destination - the destination which the subject need to arrive at. 
	 * @param initialOrder - The initial function that describes the order.
	 * @param game - the {@link PirateGame}
	 */
	public PirateOrders(int subjectId, MapObject destination, BiConsumer<PirateGame, PirateOrders> initialOrder, PirateGame game){
		this.subject = game.getMyPirateById(subjectId);
		this.subjectId = subjectId;
		path = new ArrayList<Location>();
		this.destination = destination;
		updateOrderType(initialOrder);
	}
	/**
	 * Clears the {@link Location} List that represents the path of the pirate
	 */
	public void clearPath(){
		path.clear();
	}
	/**
	 * Updates the order given to the pirate
	 * @param order - The function that describes the order. 
	 */
	public void updateOrderType(BiConsumer<PirateGame, PirateOrders> order){
		this.order = order;
		this.completed = false;
	}
	/**
	 * Called periodically, updates subject
	 * @param game - a given {@link PirateGame}
	 */
	public void update(PirateGame game){
		//if(!game.getMyLivingAircrafts().contains(subject)) return;
		
		subject = game.getMyPirateById(this.subjectId);
		path.add(subject.location);
		

		order.accept(game, this);
		
	}
	/**
	 * Gets String representation of the order function for comparing with other orders
	 * @return String representation of the order function
	 */
	public String getOrderType(){
		return this.order.toString();
	}
	/**
	 * Updates destination for designated pirate
	 * @param destination - the destination of the pirate ship. 
	 */
	public void updateDestination(MapObject destination){
		this.destination = destination;
		this.completed = false;
	}
	/**
	 * Checks if order is complete
	 * @return A boolean that represents if the order is complete.
	 */
	public boolean isComplete(){
		return this.completed;
	}
	/**
	 * A collection of convenient pirate orders.
	 * @author Yoav
	 */
	static class PirateOrderType{
		
		/**
		 * Orders when attacking. Called periodically. 
		 */
		public static BiConsumer<PirateGame, PirateOrders> attack = (game, pirateOrder) -> {
			List<Location> possibleLocations = new ArrayList<Location>();
			for(int i = -MyBot.k_SAIL_RANGE; i <= MyBot.k_SAIL_RANGE; i++) 
				for(int j = -(MyBot.k_SAIL_RANGE - Math.abs(i)); j <= MyBot.k_SAIL_RANGE - Math.abs(i); j++){
				possibleLocations.add(new Location(pirateOrder.subject.location.row + j,
						pirateOrder.subject.location.col + i));
			} // List all possible locations
			
			Comparator<Location> rankLocation = (Location loc1, Location loc2) -> { // Sort & prioritize possible locations
				// Note: Pirate will not avoid contact with enemy pirates if the fight is winnable or will result in a draw
				int score = 0;
				boolean loc1Compromised = false;
				boolean loc2Compromised = false;
				Location dest = pirateOrder.destination.getLocation();
				if(loc1.distance(dest) > loc2.distance(dest)) score+=2;
				else score-=2;
				for(Pirate enemy : game.getAllEnemyPirates()) // Calc danger levels for loc1
					if(loc1.inRange(enemy, MyBot.k_ATTACK_RANGE)) {score++; loc1Compromised = true;} 	
				for(Pirate ally : game.getAllMyPirates()) // Calc ally support for loc1
					if(loc1Compromised && !ally.equals(pirateOrder.subject) && loc1.inRange(ally, MyBot.k_SUPPORT_RANGE)) score --;	
				for(Pirate enemy : game.getAllEnemyPirates()) // Calc danger levels for loc2
					if(loc2.inRange(enemy, MyBot.k_ATTACK_RANGE)) {score--; loc2Compromised = true;}
				for(Pirate ally : game.getAllMyPirates()) // Calc ally support for loc2
					if(loc2Compromised && !ally.equals(pirateOrder.subject) && loc2.inRange(ally, MyBot.k_SUPPORT_RANGE)) score ++;	
				 
				return score;
			};
			Pirate toAttack = null;
			int enemyCount = 0;
			int allyCount = 0;
			for(Pirate enemy : game.getAllEnemyPirates()){ // Count enemy pirates in range
				if(enemy.inAttackRange(pirateOrder.subject)){
					enemyCount++;
					if(toAttack == null) toAttack = enemy;
					else continue;
					
					
				}
			}
			for(Pirate ally : game.getAllMyPirates())
				if(pirateOrder.subject.inRange(ally, MyBot.k_SUPPORT_RANGE)) allyCount++; // Count ally pirate in support range
			
			if(allyCount >= enemyCount && toAttack != null) {
				game.attack(pirateOrder.subject, toAttack); // Disengage if outnumbered
				return;
			}
			
			for(Drone enemyDrone : game.getEnemyLivingDrones()){ // Attack drones if subject is close to destination
				if(pirateOrder.subject.inAttackRange(enemyDrone) && pirateOrder.subject.distance(pirateOrder.destination) < 4){
					game.attack(pirateOrder.subject, enemyDrone);
					return;
				}
			}
			
			possibleLocations.remove(pirateOrder.subject.location); // Remove current location from list of possible locations
			possibleLocations.sort(rankLocation); // Sort locations and set sail to most optimal location
			game.setSail(pirateOrder.subject, possibleLocations.get(0)); 
			if(pirateOrder.destination.distance(pirateOrder.subject) == 0) pirateOrder.completed = true; // Check whether order is complete
			game.debug(String.valueOf(pirateOrder.subjectId) + possibleLocations);
		}; 
		/**
		 * Orders when defending. Called periodically. 
		 */
		public static BiConsumer<PirateGame, PirateOrders> defend = (game, pirateOrder) -> {
			
		}; 
		
	}
}


