package lib

object Hacks {
  val runningOnK8s: Boolean = sys.env.contains("FLOW_KUBERNETES_POD_NAME")
}
