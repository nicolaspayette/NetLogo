// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.agent

import org.nlogo.api.TrailDrawerInterface

// The vars and methods in this track the rendering state of the world.
// They should be considered transient and equality should not take them into account.
trait GrossWorldState extends WorldKernel { this: CoreWorld =>
  private[agent] var rootsTable: RootsTable = _

  // possibly need another array for 3D colors
  // since it seems messy to collapse 3D array into 2D
  protected var _patchColors: Array[Int] = _
  def patchColors: Array[Int] = _patchColors

  // this is used by the OpenGL texture code to decide whether
  // it needs to make a new texture or not - ST 2/9/05
  protected var _patchColorsDirty: Boolean = true
  def patchColorsDirty: Boolean = _patchColorsDirty
  private[agent] def patchColorsDirty(dirty: Boolean): Unit = { _patchColorsDirty = dirty }
  def markPatchColorsDirty(): Unit = { _patchColorsDirty = true }
  def markPatchColorsClean(): Unit = { _patchColorsDirty = false }

  // performance optimization -- avoid drawing an all-black bitmap if we
  // could just paint one big black rectangle
  protected var _patchesAllBlack = true
  def patchesAllBlack: Boolean = _patchesAllBlack
  private[agent] def patchesAllBlack(areBlack: Boolean): Unit = { _patchesAllBlack = areBlack }

  // for efficiency in Renderer
  protected var _patchesWithLabels: Int = 0
  def patchesWithLabels: Int = _patchesWithLabels
  private[agent] def addPatchLabel(): Unit = { _patchesWithLabels += 1 }
  private[agent] def removePatchLabel(): Unit = { _patchesWithLabels -= 1 }

  /// patch scratch
  //  a scratch area that can be used by commands such as _diffuse
  protected var _patchScratch: Array[Array[Double]] = _
  def getPatchScratch: Array[Array[Double]] = {
    if (_patchScratch == null) {
      _patchScratch = Array.ofDim[Double](_worldWidth, _worldHeight)
    }
    _patchScratch
  }

  // performance optimization for 3D renderer -- avoid sorting by distance
  // from observer unless we need to.  once this flag becomes true, we don't
  // work as hard as we could to return it back to false, because doing so
  // would be expensive.  we just reset it at clear-all time.
  protected var _mayHavePartiallyTransparentObjects = false
  def mayHavePartiallyTransparentObjects: Boolean = _mayHavePartiallyTransparentObjects
  private[agent] def mayHavePartiallyTransparentObjects(have: Boolean): Unit = {
    _mayHavePartiallyTransparentObjects = have
  }

  abstract override def clearAll(): Unit = {
    _patchesAllBlack = true
    _mayHavePartiallyTransparentObjects = false
  }

  // the trail drawer isn't as gross as the rest of the state and might actually be
  // *necessary* in a way that the rest of this trait isn't
  def trailDrawer(trailDrawer: TrailDrawerInterface): Unit = {
    _trailDrawer = trailDrawer
  }

  def trailDrawer = _trailDrawer
  private var _trailDrawer: TrailDrawerInterface = _
  def markDrawingClean(): Unit = {
    _trailDrawer.sendPixels(false)
  }
  def getDrawing: AnyRef = _trailDrawer.getDrawing
  def sendPixels: Boolean = _trailDrawer.sendPixels
}
