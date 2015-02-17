package org.reclipse.tracer.ui.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.reclipse.tracer.ui.TracerUIImages;


/**
 * @author lowende
 * @author Last editor: $Author: lowende $
 * @version $Revision: 3786 $ $Date: 2007-09-09 23:44:20 +0200 (So, 09 Sep 2007) $
 */
public class CollapseExecutionViewAction extends Action
{

   private final TreeViewer treeViewer;


   public CollapseExecutionViewAction(final TreeViewer treeViewer)
   {
      this.treeViewer = treeViewer;

      setText("Collapse");
      setToolTipText("Collapse All Classes");
      setImageDescriptor(TracerUIImages
            .getDescriptor(TracerUIImages.IMG_COLLAPSE_ALL));
   }


   /**
    * @see org.eclipse.jface.action.IAction#run()
    */
   @Override
   public void run()
   {
      this.treeViewer.collapseAll();
   }

}
