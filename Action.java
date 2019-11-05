/*
Action: ideally what our various entities might do in our virutal world
 */

import java.util.Optional;

final class Action
{
   private ActionKind kind;
   private Entity entity;
   private WorldModel world;
   private ImageStore imageStore;
   private int repeatCount;

   private final String FISH_ID_PREFIX = "fish -- ";
   private static final int FISH_CORRUPT_MIN = 20000;
   private static final int FISH_CORRUPT_MAX = 30000;
   private static final String FISH_KEY = "fish";

   private final String CRAB_KEY = "crab";
   private final String CRAB_ID_SUFFIX = " -- crab";
   private final int CRAB_PERIOD_SCALE = 4;
   private final int CRAB_ANIMATION_MIN = 50;
   private final int CRAB_ANIMATION_MAX = 150;
   private static final String QUAKE_KEY = "quake";

   public Action(ActionKind kind, Entity entity, WorldModel world,
                 ImageStore imageStore, int repeatCount)
   {
      this.kind = kind;
      this.entity = entity;
      this.world = world;
      this.imageStore = imageStore;
      this.repeatCount = repeatCount;
   }

   public void executeAction(EventScheduler scheduler)
   {
      switch (kind)
      {
         case ACTIVITY:
            executeActivityAction(scheduler);
            break;

         case ANIMATION:
            executeAnimationAction(scheduler);
            break;
      }
   }

   private void executeAnimationAction(EventScheduler scheduler)
   {
      entity.nextImage();

      if (repeatCount != 1)
      {
         scheduler.scheduleEvent(entity, createAnimationAction(entity, Math.max(repeatCount - 1, 0)),
                 entity.getAnimationPeriod());
      }
   }

   private void executeActivityAction(EventScheduler scheduler)
   {
      switch (this.entity.getKind())
      {
         case OCTO_FULL:
            executeOctoFullActivity(this.entity, this.world,
                    this.imageStore, scheduler);
            break;

         case OCTO_NOT_FULL:
            executeOctoNotFullActivity(this.entity, this.world,
                    this.imageStore, scheduler);
            break;

         case FISH:
            executeFishActivity(this.entity, this.world, this.imageStore, scheduler);
            break;

         case CRAB:
            executeCrabActivity(this.entity, this.world,
                    this.imageStore, scheduler);
            break;

         case QUAKE:
            executeQuakeActivity(this.entity, this.world, this.imageStore,
                    scheduler);
            break;

         case SGRASS:
            executeSgrassActivity(this.entity, this.world, this.imageStore,
                    scheduler);
            break;

         case ATLANTIS:
            executeAtlantisActivity(this.entity, this.world, this.imageStore,
                    scheduler);
            break;

         default:
            throw new UnsupportedOperationException(
                    String.format("executeActivityAction not supported for %s",
                            this.entity.getKind()));
      }
   }

   public void executeFishActivity(Entity entity, WorldModel world, ImageStore imageStore, EventScheduler scheduler)
   {
      Point pos = entity.getPosition();  // store current position before removing

      entity.removeEntity(world, entity);
      scheduler.unscheduleAllEvents(entity);

      Entity crab = entity.createCrab(entity.getId() + CRAB_ID_SUFFIX,
              pos, entity.getActionPeriod() / CRAB_PERIOD_SCALE,
              CRAB_ANIMATION_MIN +
                      entity.rand.nextInt(CRAB_ANIMATION_MAX - CRAB_ANIMATION_MIN),
              imageStore.getImageList(CRAB_KEY));

      world.addEntity(crab);
      scheduler.scheduleActions(crab, world, imageStore);
   }


   public void executeOctoFullActivity(Entity entity, WorldModel world,
                                       ImageStore imageStore, EventScheduler scheduler)
   {
      Optional<Entity> fullTarget = world.findNearest(entity.getPosition(), EntityKind.ATLANTIS);

      if (fullTarget.isPresent() &&
              world.moveToFull(entity, fullTarget.get(), scheduler))
      {
         //at atlantis trigger animation
         scheduler.scheduleActions(fullTarget.get(), world, imageStore);

         //transform to unfull
         world.transformFull(entity, scheduler, imageStore);
      }
      else
      {
         scheduler.scheduleEvent(entity,
                 createActivityAction(entity, world, imageStore),
                 entity.getActionPeriod());
      }
   }

   public void executeOctoNotFullActivity(Entity entity, WorldModel world, ImageStore imageStore, EventScheduler scheduler)
   {
      Optional<Entity> notFullTarget = world.findNearest(entity.getPosition(),
              EntityKind.FISH);


      if (!notFullTarget.isPresent() ||
              !world.moveToNotFull(entity, notFullTarget.get(), scheduler) ||
              !world.transformNotFull(entity, scheduler, imageStore))
      {
         scheduler.scheduleEvent(entity,
                 createActivityAction(entity, world, imageStore),
                 entity.getActionPeriod());
      }
   }

   public void executeSgrassActivity(Entity entity, WorldModel world, ImageStore imageStore, EventScheduler scheduler)
   {
      Optional<Point> openPt = world.findOpenAround(entity.getPosition());

      if (openPt.isPresent())
      {
         Entity fish = entity.createFish(FISH_ID_PREFIX + entity.getId(),
                 openPt.get(), FISH_CORRUPT_MIN + entity.rand.nextInt(FISH_CORRUPT_MAX - FISH_CORRUPT_MIN),
                 imageStore.getImageList(FISH_KEY));
         world.addEntity(fish);
         scheduler.scheduleActions(fish, world, imageStore);
      }

      scheduler.scheduleEvent(entity,
              createActivityAction(entity, world, imageStore),
              entity.getActionPeriod());
   }
   public void executeCrabActivity(Entity entity, WorldModel world, ImageStore imageStore, EventScheduler scheduler)
   {
      Optional<Entity> crabTarget = world.findNearest(entity.getPosition(), EntityKind.SGRASS);
      long nextPeriod = entity.getActionPeriod();

      if (crabTarget.isPresent())
      {
         Point tgtPos = crabTarget.get().getPosition();

         if (world.moveToCrab(entity, crabTarget.get(), scheduler))
         {
            Entity quake = entity.createQuake(tgtPos, imageStore.getImageList(QUAKE_KEY));

            world.addEntity(quake);
            nextPeriod += entity.getActionPeriod();
            scheduler.scheduleActions(quake, world, imageStore);
         }
      }
      scheduler.scheduleEvent(entity, createActivityAction(entity, world, imageStore),
              nextPeriod);
   }

   public void executeQuakeActivity(Entity entity, WorldModel world,
                                    ImageStore imageStore, EventScheduler scheduler)
   {
      scheduler.unscheduleAllEvents(entity);
      entity.removeEntity(world, entity);
   }

   public void executeAtlantisActivity(Entity entity, WorldModel world, ImageStore imageStore, EventScheduler scheduler)
   {
      scheduler.unscheduleAllEvents(entity);
      entity.removeEntity(world,entity);
   }
   public static Action createAnimationAction(Entity entity, int repeatCount)
   {
      return new Action(ActionKind.ANIMATION, entity, null, null, repeatCount);
   }

   public static Action createActivityAction(Entity entity, WorldModel world, ImageStore imageStore)
   {
      return new Action(ActionKind.ACTIVITY, entity, world, imageStore, 0);
   }
}