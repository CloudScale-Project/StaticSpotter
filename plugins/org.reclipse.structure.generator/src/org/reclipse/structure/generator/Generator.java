package org.reclipse.structure.generator;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.fujaba.commons.console.IReportListener;
import org.reclipse.structure.generator.steps.AnnotateStep;
import org.reclipse.structure.generator.steps.FindAdditionalElementsStep;
import org.reclipse.structure.generator.steps.FindSetFragmentsStep;
import org.reclipse.structure.generator.util.Constants;
import org.reclipse.structure.generator.util.EcoreUtil;
import org.reclipse.structure.generator.util.IGenerator;
import org.reclipse.structure.generator.util.NameUtil;
import org.reclipse.structure.inference.annotations.AnnotationsPackage;
import org.reclipse.structure.specification.ModifierType;
import org.reclipse.structure.specification.PSCatalog;
import org.reclipse.structure.specification.PSCombinedFragment;
import org.reclipse.structure.specification.PSCombinedFragmentItem;
import org.reclipse.structure.specification.PSMetricConstraint;
import org.reclipse.structure.specification.PSNode;
import org.reclipse.structure.specification.PSNodeConstraint;
import org.reclipse.structure.specification.PSPatternSpecification;
import org.reclipse.structure.specification.SpecificationFactory;
import org.reclipse.structure.specification.util.SpecificationUtil;
import org.reclipse.structure.specification.util.TriggerManager;
import org.storydriven.storydiagrams.StorydiagramsPackage;
import org.storydriven.storydiagrams.activities.ActivitiesFactory;
import org.storydriven.storydiagrams.activities.Activity;
import org.storydriven.storydiagrams.activities.OperationExtension;


public class Generator implements IGenerator, Constants
{
   private final IReportListener reporter;

   private final Map<PSPatternSpecification, EClass> annotations;

   private final Map<PSPatternSpecification, EClass> engines;


   private final Map<PSPatternSpecification, Map<String, Activity>> activities;

   private final AnnotateStep annotateGenerator;

   private final FindSetFragmentsStep setsGenerator;


   private final FindAdditionalElementsStep additionalsGenerator;


   private TriggerManager triggerManager;


   public Generator(IReportListener reporter)
   {
//      StorydiagramsPackage p = StorydiagramsPackage.eINSTANCE;
      this.reporter = reporter;

      annotateGenerator = new AnnotateStep(this);
      setsGenerator = new FindSetFragmentsStep(this);
      additionalsGenerator = new FindAdditionalElementsStep(this);

      annotations = new HashMap<PSPatternSpecification, EClass>();
      engines = new HashMap<PSPatternSpecification, EClass>();
      activities = new HashMap<PSPatternSpecification, Map<String, Activity>>();
   }


   public void generate(Collection<EObject> container, PSCatalog catalog)
   {
      EPackage annotationPackage = createAnnotationsPackage(container);

      createAnnotationClasses(catalog, annotationPackage);

      triggerManager = new TriggerManager(this, catalog.getPatternSpecifications());

      EPackage enginesPackage = createEnginesPackage(container);

      createEngineClasses(catalog, enginesPackage);
   }


   /**
    * Create engine classes.
    * 
    * @param catalog
    * @param enginesPackage
    */
   private void createEngineClasses(PSCatalog catalog, EPackage enginesPackage)
   {
      for (PSPatternSpecification pattern : catalog.getPatternSpecifications())
      {
         if (!pattern.isAbstract())
         {
            workaroundSets(pattern);
            workaroundNegatives(pattern);

            // translate pattern into class with story patterns
            addEngineClass(enginesPackage, pattern);

            revertWorkaroundSets(pattern);
            revertWorkaroundNegatives(pattern);
         }
      }
   }


   /**
    * Create the engines package.
    * 
    * @param container
    * @return
    */
   private EPackage createEnginesPackage(Collection<EObject> container)
   {
      debug("Creating package '%1s'...", PACKAGE_NAME_ENGINES);
      EPackage enginesPackage = EcoreUtil.addEPackage(container, PACKAGE_NAME_ENGINES, PACKAGE_URI_ENGINES);
      return enginesPackage;
   }


   /**
    * Create annotation classes.
    * 
    * @param catalog
    * @param annotationPackage
    */
   private void createAnnotationClasses(PSCatalog catalog, EPackage annotationPackage)
   {
      for (PSPatternSpecification pattern : catalog.getPatternSpecifications())
      {
         addAnnotationClass(annotationPackage, pattern);
      }
   }


   /**
    * Create the annotations package.
    * 
    * @param container
    * @return
    */
   private EPackage createAnnotationsPackage(Collection<EObject> container)
   {
      debug("Creating package '%1s'...", PACKAGE_NAME_ANNOTATIONS);
      EPackage annotationPackage = EcoreUtil.addEPackage(container, PACKAGE_NAME_ANNOTATIONS, PACKAGE_URI_ANNOTATIONS);
      return annotationPackage;
   }


   private EClass addAnnotationClass(EPackage container, PSPatternSpecification pattern)
   {
      // check if the class is already created
      if (annotations.containsKey(pattern))
      {
         return annotations.get(pattern);
      }

      // make name
      String name = pattern.getName() + SUFFIX_ANNOTATION;

      // create class
      debug("Creating class '%1s'...", name);
      EClass element = EcoreUtil.addEClass(container, name);

      // cache it
      annotations.put(pattern, element);

      // get super type
      EClass superType = AnnotationsPackage.Literals.ASG_ANNOTATION;

      // create super annotation first
      if (pattern.getSuperPattern() != null)
      {
         superType = addAnnotationClass(container, pattern.getSuperPattern());
      }

      element.getESuperTypes().add(superType);

      return element;
   }


   private void addEngineClass(EPackage container, PSPatternSpecification pattern)
   {
      // make name
      String name = pattern.getName() + SUFFIX_ENGINE;

      // create engine class
      debug("Creating class '%1s'...", pattern.getName() + SUFFIX_ENGINE);
      EClass engineClass = EcoreUtil.addEClass(container, name);
      engines.put(pattern, engineClass);
      engineClass.getESuperTypes().add(AnnotationsPackage.Literals.ANNOTATION_ENGINE);

      // create methods and activities
      if (setsContainedIn(pattern))
      {
         Activity findSetFragements = createFindSets(engineClass, pattern);
         setsGenerator.generate(findSetFragements, pattern);
      }

      if (SpecificationUtil.isAdditionalElements(pattern))
      {
         Activity findAdditionalElements = createFindAdditionals(engineClass, pattern);
         additionalsGenerator.generate(findAdditionalElements, pattern);
      }

      // generate main method
      Activity annotate = createAnnotate(engineClass, pattern);
      annotateGenerator.generate(annotate, pattern);
   }


   private boolean setsContainedIn(PSPatternSpecification pattern)
   {
      return SpecificationUtil.isSetSearchRequired(pattern);
   }


   /**
    * Replace every set node in the given pattern with a set fragment that contains that node. The
    * set modifier is then removed. This workaround allows to handle set nodes and fragments
    * uniformly (i.e. as set fragments).
    * 
    * @param pattern The current pattern specification in which the set nodes are replaced.
    */
   private void workaroundSets(PSPatternSpecification pattern)
   {
      for (PSNode node : pattern.getNodes())
      {
         if (ModifierType.SET.equals(node.getModifier()))
         {
            node.setModifier(ModifierType.NONE);

            PSCombinedFragment wrapper = SpecificationFactory.eINSTANCE.createPSCombinedFragment();

            wrapper.setKind(ModifierType.SET);
            wrapper.setName(node.getName() + SUFFIX_WRAPPER);
            wrapper.setWeight(node.getWeight());
            wrapper.setPatternSpecification(pattern);

            wrapper.getChildren().add(node);

            PSNodeConstraint constraint = null;
            for (PSNodeConstraint nodeConstraint : node.getNodeConstraints())
            {
               if (constraint != null)
               {
                  warn("Could not wrap set node '%1s' with a fragment: Found more than one SIZE constraint!",
                        node.getName());
               }
               if (nodeConstraint instanceof PSMetricConstraint
                     && "SIZE".equals(((PSMetricConstraint) nodeConstraint).getMetricAcronym()))
               {
                  constraint = nodeConstraint;
               }
            }
            node.getNodeConstraints().remove(constraint);
            wrapper.setConstraint(constraint);
         }
      }
   }


   /**
    * Replace every negative object in the given pattern with a negative fragment that contains that
    * node. The negative modifier is then removed. This workaround allows to handle negative objects
    * and fragments uniformly (i.e. as negative fragments).
    * 
    * @param pattern The current pattern specification in which the negative objects are replaced.
    */
   private void workaroundNegatives(PSPatternSpecification pattern)
   {
      for (PSNode node : pattern.getNodes())
      {
         if (ModifierType.NEGATIVE.equals(node.getModifier()))
         {
            node.setModifier(ModifierType.NONE);

            PSCombinedFragment wrapper = SpecificationFactory.eINSTANCE.createPSCombinedFragment();

            wrapper.setKind(ModifierType.NEGATIVE);
            wrapper.setName(node.getName() + SUFFIX_WRAPPER);
            wrapper.setWeight(node.getWeight());

            wrapper.getChildren().add(node);

            PSNodeConstraint constraint = null;
            for (PSNodeConstraint nodeConstraint : node.getNodeConstraints())
            {
               if (constraint != null)
               {
                  warn("Could not wrap negative node '%1s' with a fragment: Found more than one SIZE constraint!",
                        node.getName());
               }
               if (nodeConstraint instanceof PSMetricConstraint
                     && "SIZE".equals(((PSMetricConstraint) nodeConstraint).getMetricAcronym()))
               {
                  constraint = nodeConstraint;
               }
            }
            node.getNodeConstraints().remove(constraint);
            wrapper.setConstraint(constraint);

            wrapper.setPatternSpecification(pattern);
         }
      }
   }


   private Activity createFindSets(EClass engineClass, PSPatternSpecification pattern)
   {
      // add the operation
      EOperation operation = EcoreUtil.addEOperation(engineClass, METHOD_FIND_SETS);
      operation.setEType(EcorePackage.Literals.EBOOLEAN);

      // add 'this' parameter
      EcoreUtil.addEParameter(operation, VAR_THIS, engineClass);

      // add 'annotation' parameter
      EcoreUtil.addEParameter(operation, VAR_ANNOTATION, AnnotationsPackage.Literals.ASG_ANNOTATION);

      return createActivity(operation, pattern);
   }


   private Activity createFindAdditionals(EClass engineClass, PSPatternSpecification pattern)
   {
      // add the operation
      EOperation operation = EcoreUtil.addEOperation(engineClass, METHOD_FIND_ADDITIONALS);

      // parameter 'this'
      EcoreUtil.addEParameter(operation, VAR_THIS, engineClass);

      // parameter 'annotation'
      EcoreUtil.addEParameter(operation, VAR_ANNOTATION, AnnotationsPackage.Literals.ASG_ANNOTATION);

      return createActivity(operation, pattern);
   }


   private Activity createAnnotate(EClass engineClass, PSPatternSpecification pattern)
   {
      // add the operation
      EOperation operation = EcoreUtil.addEOperation(engineClass, METHOD_ANNOTATE);
      operation.setEType(AnnotationsPackage.Literals.ANNOTATION_SET);

      // add 'this' parameter
      EcoreUtil.addEParameter(operation, VAR_THIS, engineClass);

      // add 'context' parameter
      EcoreUtil.addEParameter(operation, VAR_CONTEXT, EcorePackage.Literals.EOBJECT);

      // add 'additional' parameter
      EcoreUtil.addEParameter(operation, VAR_SEARCH_ADDITIONALS, EcorePackage.Literals.EBOOLEAN);

      return createActivity(operation, pattern);
   }


   @Override
   public Activity createActivity(EOperation operation, PSPatternSpecification pattern)
   {
      // create activity
      Activity activity = ActivitiesFactory.eINSTANCE.createActivity();
      activity.setName(NameUtil.getName(operation));
      activity.setComment(NameUtil.getName(operation));

      // add operation extension
      OperationExtension extension = ActivitiesFactory.eINSTANCE.createOperationExtension();
      extension.setOwnedActivity(activity);
      extension.setOperation(operation);

      activity.getInParameters().addAll(operation.getEParameters());

      // configure out parameter (should be ONLY ONE)
      assert (extension.getOutParameters().size() <= 1) : "Activities should only have one out parameter.";
      // TODO: The following commented code caused an exception. The default value for the out
      // parameters should be enough here, shouldnt it?
      
      // for (EParameter outParam : extension.getOutParameters())
      // {
      // outParam.setName(VAR_METHOD_RETURN_VALUE);
      // activity.getOutParameters().add(outParam);
      // }

      // cache it
      Map<String, Activity> cachedActivities = activities.get(pattern);
      if (cachedActivities == null)
      {
         cachedActivities = new HashMap<String, Activity>();
         activities.put(pattern, cachedActivities);
      }
      cachedActivities.put(operation.getName(), activity);

      return activity;
   }


   private void revertWorkaroundSets(PSPatternSpecification pattern)
   {
      Collection<PSCombinedFragment> toRemove = new ArrayList<PSCombinedFragment>();
      for (PSCombinedFragment fragment : pattern.getCombinedFragments())
      {
         if (ModifierType.SET.equals(fragment.getKind()) && fragment.getName().endsWith(SUFFIX_WRAPPER)
               && fragment.getChildren().size() == 1)
         {
            for (PSCombinedFragmentItem item : fragment.getChildren())
            {
               if (item instanceof PSNode && ModifierType.NONE.equals(((PSNode) item).getModifier()))
               {
                  PSNode node = (PSNode) item;

                  node.setModifier(ModifierType.SET);
                  node.setWeight(fragment.getWeight());

                  PSNodeConstraint constraint = fragment.getConstraint();
                  if (constraint != null)
                  {
                     fragment.setConstraint(null);
                     node.getNodeConstraints().add(constraint);
                  }
               }
            }
            toRemove.add(fragment);
         }
      }

      pattern.getCombinedFragments().removeAll(toRemove);
   }


   private void revertWorkaroundNegatives(PSPatternSpecification pattern)
   {
      Collection<PSCombinedFragment> toRemove = new ArrayList<PSCombinedFragment>();
      for (PSCombinedFragment fragment : pattern.getCombinedFragments())
      {
         if (ModifierType.NEGATIVE.equals(fragment.getKind()) && fragment.getName().endsWith(SUFFIX_WRAPPER)
               && fragment.getChildren().size() == 1)
         {
            for (PSCombinedFragmentItem item : fragment.getChildren())
            {
               if (item instanceof PSNode && ModifierType.NONE.equals(((PSNode) item).getModifier()))
               {
                  PSNode node = (PSNode) item;

                  node.setModifier(ModifierType.NEGATIVE);
                  node.setWeight(fragment.getWeight());

                  PSNodeConstraint constraint = fragment.getConstraint();
                  if (constraint != null)
                  {
                     fragment.setConstraint(null);
                     node.getNodeConstraints().add(constraint);
                  }
               }
            }
            toRemove.add(fragment);
         }
      }

      pattern.getCombinedFragments().removeAll(toRemove);
   }


   @Override
   public EClass getAnnotationClass(PSPatternSpecification pattern)
   {
      return annotations.get(pattern);
   }


   @Override
   public EClass getEngineClass(PSPatternSpecification pattern)
   {
      return engines.get(pattern);
   }


   @Override
   public Activity getActivity(PSPatternSpecification pattern, String name)
   {
      Map<String, Activity> cached = activities.get(pattern);
      if (cached != null)
      {
         return cached.get(name);
      }

      return null;
   }


   @Override
   public PSNode getTrigger(PSPatternSpecification pattern)
   {
      return triggerManager.getTrigger(pattern);
   }


   @Override
   public IStatus error(String message, Object... args)
   {
      return reporter.error(message, args);
   }


   @Override
   public void warn(String message, Object... args)
   {
      reporter.warn(message, args);
   }


   @Override
   public void append(String message, Object... args)
   {
      reporter.append(message, args);
   }


   @Override
   public void task(String message, Object... args)
   {
      reporter.task(message, args);
   }


   @Override
   public void info(String message, Object... args)
   {
      reporter.info(message, args);
   }


   @Override
   public void debug(String message, Object... args)
   {
      reporter.debug(message, args);
   }
}
