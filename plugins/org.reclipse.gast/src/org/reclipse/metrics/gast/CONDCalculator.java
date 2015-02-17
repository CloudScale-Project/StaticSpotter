package org.reclipse.metrics.gast;


import org.eclipse.emf.ecore.EObject;
import org.reclipse.metrics.AbstractGASTMetricCalculator;

import de.fzi.gast.core.Package;
import de.fzi.gast.core.Root;
import de.fzi.gast.functions.Function;
import de.fzi.gast.types.GASTClass;


/**
 * This metric calculates the number of conditional statements (COND) for different elements.
 * Supported elements are elements or sub types of the following:
 * 
 * <dl>
 * <dt>{@link Root Project}</dt>
 * <dd>The total COND for all contained packages.</dd>
 * <dt>{@link Package Package}</dt>
 * <dd>The total COND for all contained classes.</dd>
 * <dt>{@link GASTClass Class}</dt>
 * <dd>The total COND for the class.</dd>
 * <dt>{@link Function Method}</dt>
 * <dd>The COND for the method.</dd>
 * </dl>
 * 
 * @version $Revision$ $Date$
 * @author Last editor: $Author$
 * @author harka
 */
public class CONDCalculator extends AbstractGASTMetricCalculator
{

   public CONDCalculator()
   {
      super();

      addClass(getCore().getRoot());
      addClass(getCore().getPackage());
      addClass(getTypes().getGASTClass());
      addClass(getFunctions().getFunction());
   }


   @Override
   public double calculate(EObject object)
   {
      if (object != null)
      {
         // FIXME: calculate COND
      }

      return -1;
   }
}
