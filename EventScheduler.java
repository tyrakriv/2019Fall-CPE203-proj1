import java.util.*;

/*
EventScheduler: ideally our way of controlling what happens in our virtual world
 */

final class EventScheduler
{
   private PriorityQueue<Event> eventQueue;
   private Map<Entity, List<Event>> pendingEvents;
   private double timeScale;

   public static final int QUAKE_ANIMATION_REPEAT_COUNT = 10;
   public static final int ATLANTIS_ANIMATION_REPEAT_COUNT = 7;

   public EventScheduler(double timeScale)
   {
      this.eventQueue = new PriorityQueue<>(new EventComparator());
      this.pendingEvents = new HashMap<>();
      this.timeScale = timeScale;
   }

   public void scheduleEvent(Entity entity, Action action, long afterPeriod)
   {
      long time = System.currentTimeMillis() +
              (long)(afterPeriod * this.timeScale);
      Event event = new Event(action, time, entity);

      this.eventQueue.add(event);

      // update list of pending events for the given entity
      List<Event> pending = this.pendingEvents.getOrDefault(entity,
              new LinkedList<>());
      pending.add(event);
      this.pendingEvents.put(entity, pending);
   }

   public void unscheduleAllEvents(Entity entity)
   {
      List<Event> pending = this.pendingEvents.remove(entity);

      if (pending != null)
      {
         for (Event event : pending)
         {
            this.eventQueue.remove(event);
         }
      }
   }

   public void removePendingEvent(Event event)
   {
      List<Event> pending = this.pendingEvents.get(event.entity);

      if (pending != null)
      {
         pending.remove(event);
      }
   }

   public void updateOnTime(long time)
   {
      while (!eventQueue.isEmpty() &&
              eventQueue.peek().time < time)
      {
         Event next = eventQueue.poll();

         removePendingEvent(next);

         next.action.executeAction(this);
      }
   }

   public void scheduleActions(Entity entity, WorldModel world, ImageStore imageStore)
   {
      switch (entity.getKind())
      {
         case OCTO_FULL: scheduleEvent(entity, Action.createActivityAction(entity, world, imageStore), entity.getActionPeriod());
            scheduleEvent(entity, Action.createAnimationAction(entity,0), entity.getAnimationPeriod());
            break;

         case OCTO_NOT_FULL:
            scheduleEvent(entity, Action.createActivityAction(entity, world, imageStore), entity.getActionPeriod());
            scheduleEvent(entity, Action.createAnimationAction(entity, 0), entity.getAnimationPeriod());
            break;

         case FISH:
            scheduleEvent(entity,
                    Action.createActivityAction(entity, world, imageStore),
                    entity.getActionPeriod());
            break;

         case CRAB:
            scheduleEvent(entity,
                    Action.createActivityAction(entity, world, imageStore),
                    entity.getActionPeriod());
            scheduleEvent(entity,
                    Action.createAnimationAction(entity, 0), entity.getAnimationPeriod());
            break;

         case QUAKE:
            scheduleEvent(entity,
                    Action.createActivityAction(entity, world, imageStore),
                    entity.getActionPeriod());
            scheduleEvent(entity,
                    Action.createAnimationAction(entity, QUAKE_ANIMATION_REPEAT_COUNT),
                    entity.getAnimationPeriod());
            break;

         case SGRASS:
            scheduleEvent(entity,
                    Action.createActivityAction(entity, world, imageStore),
                    entity.getActionPeriod());
            break;
         case ATLANTIS:
            scheduleEvent(entity,
                    Action.createAnimationAction(entity, ATLANTIS_ANIMATION_REPEAT_COUNT),
                    entity.getAnimationPeriod());
            break;

         default:
      }
   }

   public void scheduleActions(WorldModel world, ImageStore imageStore)
   {
      for (Entity entity : world.getEntities())
      {
         //Only start actions for entities that include action (not those with just animations)
         if (entity.getActionPeriod() > 0)
            this.scheduleActions(entity, world, imageStore);
      }
   }












}