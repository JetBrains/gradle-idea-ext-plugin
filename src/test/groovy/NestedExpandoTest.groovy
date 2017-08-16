import groovy.json.JsonOutput
import org.jetbrains.gradle.ext.NestedExpando
import spock.lang.Specification

/**
 * Created by Nikita.Skvortsov
 * date: 16.08.2017.
 */
class NestedExpandoTest extends Specification {
  def "simple attributes are added"() {
    given:
    def obj = new NestedExpando()

    when:
    obj.configure {
      k1 "v1"
      k2 "v2","v3"
    }

    then:
    JsonOutput.toJson(obj) == '{"k1":"v1","k2":["v2","v3"]}'
  }

  def "nested object properly created"() {
    given:
    def obj = new NestedExpando()

    when:
    obj.configure {
      k1 "v1"
      k2 {
        sub_k1 "sub_v1"
        sub_k2 "sub_v2"
      }
    }

    then:
    JsonOutput.toJson(obj) == '{"k1":"v1","k2":{"sub_k1":"sub_v1","sub_k2":"sub_v2"}}'
  }

  def "missing attributes are resolved from outer scopes"() {
    given:
    def obj = new NestedExpando()

    when:
    def global = "global_val"
    obj.configure {
      def outer = "outer_val"
      k1 {
        def inner = "inner_val"
        sk1 = "$inner"
        sk2 = "$outer"
        sk3 = "$global"
      }
    }

    then:
    JsonOutput.toJson(obj) == '{"k1":{"sk1":"inner_val","sk2":"outer_val","sk3":"global_val"}}'
  }
}
