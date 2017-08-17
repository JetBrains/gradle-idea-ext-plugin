package org.jetbrains.gradle.ext

import org.gradle.util.Configurable

class NestedExpando extends Expando implements Configurable<NestedExpando>  {


  @Override
  NestedExpando configure(Closure closure) {
    def cloned = closure.clone()
    cloned.setDelegate(this)
    cloned.setResolveStrategy(Closure.DELEGATE_FIRST)
    cloned.call()
    return this
  }

  @Override
  Object invokeMethod(String name, Object args) {
    if (args instanceof Object[] && args.length == 1 && args[0] instanceof Closure) {
      def nested = new NestedExpando()
      nested.configure(args[0] as Closure)
      setProperty(name, nested)
      return nested
    } else if (args instanceof Object[] && args.length == 1) {
      setProperty(name, args[0])
      return args[0]
    } else {
      setProperty(name, args)
      return args
    }
  }

  @Override
  Object getProperty(String property) {
    Object result = super.getProperty(property)
    if (result != null) {
      return result
    } else {
      throw new MissingPropertyException(property, NestedExpando)
    }
  }

  @Override
  protected Map createMap() {
    return new LinkedHashMap()
  }
}