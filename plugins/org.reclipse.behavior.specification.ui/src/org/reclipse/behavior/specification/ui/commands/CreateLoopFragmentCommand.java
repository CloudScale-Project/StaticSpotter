package org.reclipse.behavior.specification.ui.commands;


import org.reclipse.behavior.specification.ui.editparts.BehavioralPatternEditPart;

import de.uni_paderborn.basicSequenceDiagram.BasicSequenceDiagramFactory;
import de.uni_paderborn.basicSequenceDiagram.CombinedFragment;


/**
 * @author mcp
 * @author Last editor: $Author$
 * @version $Revision$ $Date$
 * 
 */
public class CreateLoopFragmentCommand extends
      AbstractCreateCombinedFragmentCommand
{

   public CreateLoopFragmentCommand(BehavioralPatternEditPart diagramEditPart)
   {
      super(diagramEditPart);
   }


   @Override
   protected CombinedFragment createCombinedFragment()
   {
      CombinedFragment fragment = BasicSequenceDiagramFactory.eINSTANCE
            .createLoopFragment();
      return fragment;
   }

}
