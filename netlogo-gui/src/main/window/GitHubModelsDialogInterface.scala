package org.nlogo.window

trait GitHubModelsDialogInterface {
  def getModel(): Option[(java.net.URI, String)]
}
