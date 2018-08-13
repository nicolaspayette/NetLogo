package org.nlogo.app.github

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.net.URI
import java.util.prefs.Preferences

import scala.util.Success
import scala.math.max

import org.nlogo.awt.Positioning
import org.nlogo.app.github.ApiClient.swingExecutionContext
import org.nlogo.swing.Utils.addEscKeyAction
import org.nlogo.window.GitHubModelsDialogInterface

import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JSplitPane.HORIZONTAL_SPLIT
import javax.swing.JTree
import javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION

case class GitHubModel(uri: URI, source: String)

class ModelsDialog(parent: Frame)
  extends JDialog(parent, "Online Models from GitHub", true)
  with TreeSelectionListener
  with GitHubModelsDialogInterface {

  def getModel(): Option[(java.net.URI, String)] = {
    tree.setSelectionRow(0)
    setVisible(true)
    println(model.get)
    model.map { m => (m.uri, m.source) }
  }

  val prefs = Preferences.userNodeForPackage(classOf[ModelsDialog])
  private var model: Option[GitHubModel] = None

  setLayout(new BorderLayout)

  val openAction = new AbstractAction("Open Model") {
    setEnabled(false)
    def actionPerformed(e: ActionEvent) {
      setVisible(false)
    }
  }
  val openButton = new JButton(openAction)
  val cancelAction = new AbstractAction("Cancel") {
    def actionPerformed(e: ActionEvent) {
      model = None
      setVisible(false)
    }
  }
  val cancelButton = new JButton(cancelAction)
  val tokenField = new JPasswordField(25)
  add(new JPanel {
    setLayout(new BorderLayout)
    add(new JPanel {
      add(new JLabel("OAuth2 token:"))
      add(tokenField)
      add(new JButton(new AbstractAction("(Re)load model list from GitHub") {
        def actionPerformed(e: ActionEvent) {
          tree.setModel(new TreeModel(client))
        }
      }))
    }, BorderLayout.LINE_START)
    add(new JPanel {
      add(openButton)
      add(cancelButton)
    }, BorderLayout.LINE_END)
  }, BorderLayout.PAGE_END)

  def valueChanged(e: TreeSelectionEvent): Unit = {
    def update(description: String, m: Option[GitHubModel]): Unit = {
      htmlPane.setText(description)
      htmlPane.setCaretPosition(0)
      model = m
      openButton.setEnabled(model.isDefined)
    }
    val tree = e.getSource.asInstanceOf[JTree]
    tree.getLastSelectedPathComponent match {
      case m: ModelNode =>
        if (!m.source.isCompleted) update(m.info, None)
        m.source.onComplete {
          case Success(source) if m eq tree.getLastSelectedPathComponent => {
            val uri = new File(m.path.split("/").last).toURI
            update(m.info, Some(GitHubModel(uri, source)))
          }
        }
      case r: RepoNode =>
        r.addChildren // trigger the lazy future
        if (!r.readme.isCompleted) update(r.info, None)
        r.readme.onComplete {
          case Success(source) if r eq tree.getLastSelectedPathComponent =>
            update(r.info, None)
        }
      case n: Node => update(n.info, None)
      case _ => // our GitHub tree is not loaded; do nothing
    }
  }

  addEscKeyAction(this, cancelAction)
  setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)
  addWindowListener(new WindowAdapter() {
    override def windowClosing(e: WindowEvent) {
      cancelAction.actionPerformed(null)
    }
  })

  val client = new ApiClient(() => tokenField.getPassword.mkString)

  implicit class RichDimension(d: Dimension) {
    def orAtLeast(width: Int, height: Int) =
      new Dimension(max(d.width, width), max(d.height, height))
  }
  val tree = new JTree(new DefaultMutableTreeNode) {
    override def getPreferredSize = super.getPreferredSize.orAtLeast(300, 500)
    setRootVisible(false)
    getSelectionModel.setSelectionMode(SINGLE_TREE_SELECTION)
  }
  tree.addTreeSelectionListener(this)

  val htmlPane = new JEditorPane() {
    override def getPreferredSize = super.getPreferredSize.orAtLeast(500, 500)
    setContentType("text/html")
    setEditable(false)
  }

  val splitPane = new JSplitPane(HORIZONTAL_SPLIT) {
    setTopComponent(new JScrollPane(tree))
    setBottomComponent(new JScrollPane(htmlPane))
    setDividerLocation(0.5)
  }

  add(splitPane, BorderLayout.CENTER)
  pack()
  Positioning.center(this, parent)
}
