/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.dse.driver.api.core.graph.statement;

// INFO: meta props are going away in NGDG

import static com.datastax.dse.driver.api.core.graph.DseGraph.g;
import static com.datastax.dse.driver.api.core.graph.FluentGraphStatement.newInstance;
import static com.datastax.dse.driver.api.core.graph.SampleGraphScripts.ALLOW_SCANS;
import static com.datastax.dse.driver.api.core.graph.SampleGraphScripts.MAKE_STRICT;
import static com.datastax.dse.driver.api.core.graph.TinkerGraphAssertions.assertThat;

import com.datastax.dse.driver.api.core.graph.GraphResultSet;
import com.datastax.dse.driver.api.core.graph.ScriptGraphStatement;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.testinfra.DseRequirement;
import com.datastax.oss.driver.api.testinfra.ccm.CustomCcmRule;
import com.datastax.oss.driver.api.testinfra.session.SessionRule;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

// INFO: meta props are going away in NGDG

@DseRequirement(min = "5.0.3", description = "DSE 5.0.3 required for remote TinkerPop support")
public class GraphTraversalMetaPropertiesIT {

  private static CustomCcmRule ccmRule = CustomCcmRule.builder().withDseWorkloads("graph").build();

  private static SessionRule<CqlSession> sessionRule =
      SessionRule.builder(ccmRule).withCreateGraph().build();

  @ClassRule public static TestRule chain = RuleChain.outerRule(ccmRule).around(sessionRule);

  /** Builds a simple schema that provides for a vertex with a property with sub properties. */
  public static String metaProps =
      MAKE_STRICT
          + ALLOW_SCANS
          + "schema.propertyKey('sub_prop').Text().create()\n"
          + "schema.propertyKey('sub_prop2').Text().create()\n"
          + "schema.propertyKey('meta_prop').Text().properties('sub_prop', 'sub_prop2').create()\n"
          + "schema.vertexLabel('meta_v').properties('meta_prop').create()";

  /**
   * Ensures that a traversal that yields a vertex with a property that has its own properties that
   * is appropriately parsed and made accessible via {@link VertexProperty#property(String)}.
   *
   * @test_category dse:graph
   */
  @Test
  public void should_parse_meta_properties() {
    sessionRule.session().execute(ScriptGraphStatement.newInstance(metaProps));

    GraphResultSet result =
        sessionRule
            .session()
            .execute(
                newInstance(
                    g.addV("meta_v")
                        .property("meta_prop", "hello", "sub_prop", "hi", "sub_prop2", "hi2")));

    Vertex v = result.one().asVertex();
    assertThat(v).hasProperty("meta_prop");

    VertexProperty<String> metaProp = v.property("meta_prop");
    assertThat(metaProp)
        .hasValue("hello")
        .hasProperty("sub_prop", "hi")
        .hasProperty("sub_prop2", "hi2");
  }
}
