// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.window

import org.nlogo.agent.{ AgentManagement, CoreWorld }
import org.nlogo.core.{ ShapeEvent, ShapeAdded, ShapeRemoved, AllShapesReplaced }

class ShapeChangeListener(workspace: GUIWorkspace, world: CoreWorld with AgentManagement) {
  private val turtleShapeTracker = world.turtleShapes
  private val linkShapeTracker = world.linkShapes
  private val turtleListener = new turtleShapeTracker.Sub {
    def notify(pub: turtleShapeTracker.Pub, event: ShapeEvent): Unit = {
      handleShapeEvent(event)
    }
  }

  private val linkListener = new linkShapeTracker.Sub {
    def notify(pub: linkShapeTracker.Pub, event: ShapeEvent): Unit = {
      handleShapeEvent(event)
    }
  }

  def handleShapeEvent(event: ShapeEvent): Unit = {
    event match {
      case ShapeAdded(_, oldShapeOption, _) => oldShapeOption.foreach(workspace.shapeChanged)
      case ShapeRemoved(removedShape, _) => workspace.shapeChanged(removedShape)
      case _ =>
        // note that the other cases aren't handled here as they happen only when a view refresh would happen anyway
    }
  }
  turtleShapeTracker.subscribe(turtleListener)
  linkShapeTracker.subscribe(linkListener)
}
