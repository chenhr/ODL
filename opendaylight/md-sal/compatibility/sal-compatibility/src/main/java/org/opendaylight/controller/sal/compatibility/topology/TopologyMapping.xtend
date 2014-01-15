package org.opendaylight.controller.sal.compatibility.topology

import com.google.common.collect.FluentIterable
import java.util.Collections
import java.util.List
import java.util.concurrent.CopyOnWriteArrayList
import org.opendaylight.controller.sal.core.ConstructionException
import org.opendaylight.controller.sal.core.Edge
import org.opendaylight.controller.sal.core.Node
import org.opendaylight.controller.sal.core.NodeConnector
import org.opendaylight.controller.sal.core.UpdateType
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.NodeId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.TpId
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.Topology
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev130712.network.topology.topology.Link

import static com.google.common.base.Preconditions.*
import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import org.opendaylight.controller.md.sal.binding.util.TypeSafeDataReader
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector

class TopologyMapping {
    
    private new() {
        throw new UnsupportedOperationException("Utility class. Instantiation is not allowed.");
    }
    
    public static def toADEdgeUpdates(Topology topology,TypeSafeDataReader reader) {
        val List<TopoEdgeUpdate> result = new CopyOnWriteArrayList<TopoEdgeUpdate>()
        return FluentIterable.from(topology.link).transform[toAdEdge(topology).toTopoEdgeUpdate(reader)].copyInto(result)
    }
    
    public static def toAdEdge(Link link,Topology topology) {
        val adSrc = link.source.sourceTp.toADNodeConnector(link.source.sourceNode)
        val adDst = link.destination.destTp.toADNodeConnector(link.destination.destNode)
        return new Edge(adSrc,adDst); 
    }
    
    public static def toTopoEdgeUpdate(Edge e,TypeSafeDataReader reader) {
        return toTopoEdgeUpdate(e,UpdateType.ADDED,reader)
    }
    
    public static def toTopoEdgeUpdate(Edge e,UpdateType type,TypeSafeDataReader reader) {
        return new TopoEdgeUpdate(e,e.toAdEdgeProperties(reader),type)
    }
    
    public static def toAdEdgeProperties(Edge e,TypeSafeDataReader reader) {
        val nc = reader.readOperationalData(e.tailNodeConnector.toNodeConnectorRef.value as InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector>)
        return nc.toADNodeConnectorProperties     
    }
    
    public static def toADNodeId(NodeId nodeId) {
        checkNotNull(nodeId);
        return nodeId.value
    }
    public static def toADNodeConnector(TpId source,NodeId nodeId) throws ConstructionException {
        checkNotNull(source);
        return new NodeConnector(MD_SAL_TYPE,source.toADNodeConnectorId,nodeId.toADNode)
    }
    
    public static def toADNodeConnectorId(TpId nodeConnectorId) {
        return nodeConnectorId.value
    }
    
    public static def toADNode(NodeId nodeId) {
        checkNotNull(nodeId);
        return new Node(MD_SAL_TYPE,nodeId.toADNodeId);       
    }
}