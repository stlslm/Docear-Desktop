package org.docear.plugin.core.util;

import java.net.URI;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.mindmapmode.MModeWorkspaceLinkController;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.attribute.AttributeView;

public class NodeUtilities {

	public static NodeModel setLinkFrom(URI file, NodeModel node) {
		MModeWorkspaceLinkController.getController().setLinkTypeDependantLink(node, file);

		return node;
	}
	
	public static URI getURI(NodeModel node) {
		return URIUtils.getAbsoluteURI(node);
	}
	
	public static URI getLink(NodeModel node) {
		return NodeLinks.getLink(node);
	}

	public static NodeModel insertChildNodeFrom(NodeModel node, boolean isLeft, NodeModel target) {
		((MMapController) Controller.getCurrentModeController().getMapController()).insertNode(node, target, false, isLeft, isLeft);

		return node;
	}

	public static boolean setAttributeValue(NodeModel target, String attributeKey, Object value) {
		try {
			if (target == null || attributeKey == null || value == null) return false;
			for (INodeView nodeView : target.getViewers()) {
				if (nodeView instanceof NodeView) {
					NodeAttributeTableModel attributes = ((NodeView) nodeView).getAttributeView().getAttributes();
					if (attributes != null) {
						if (attributes.getAttributeKeyList().contains(attributeKey)) {
							// int pos =
							// attributes.getAttributePosition(attributeKey);
							AttributeController.getController(MModeController.getMModeController()).performSetValueAt(attributes, value,
									attributes.getAttributePosition(attributeKey), 1);
							// attributes.setValue(pos,value);
							// attributes.fireTableRowsUpdated(pos, pos);
						}
						else {
							AttributeController.getController(MModeController.getMModeController()).performInsertRow(attributes, attributes.getRowCount(),
									attributeKey, value);
							// attributes.addRowNoUndo(new
							// Attribute(attributeKey, value));
						}

						AttributeView attributeView = (((MapView) Controller.getCurrentController().getMapViewManager().getMapViewComponent()).getSelected())
								.getAttributeView();
						attributeView.getContainer().invalidate();
						attributeView.update();
						return true;
					}
				}
			}
		}
		catch (Exception e) {
			LogUtils.warn("org.docear.plugin.pdfutilities.util.NodeUtils.setAttributeValue(1): " + e.getMessage());
		}
		return false;
	}

	public static void removeAttribute(NodeModel target, String attributeKey) {
		if (target == null || attributeKey == null) {
			return;
		}
		for (INodeView nodeView : target.getViewers()) {
			if (nodeView instanceof NodeView) {
				NodeAttributeTableModel attributes = ((NodeView) nodeView).getAttributeView().getAttributes();
				if (attributes != null && attributes.getAttributeKeyList().contains(attributeKey)) {
					AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributes,
							attributes.getAttributePosition(attributeKey));
				}
				if (attributes.getRowCount() <= 0) {
					((NodeView) nodeView).getAttributeView().viewRemoved();
				}
			}
		}
		// NodeAttributeTableModel attributes =
		// AttributeController.getController(MModeController.getMModeController()).createAttributeTableModel(target);
		// if(attributes != null &&
		// attributes.getAttributeKeyList().contains(attributeKey)) {
		// AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributes,
		// attributes.getAttributePosition(attributeKey));
		// }
	}

	public static void removeAttributes(NodeModel target) {
		if (target == null) {
			return;
		}
		for (INodeView nodeView : target.getViewers()) {
			if (nodeView instanceof NodeView) {
				NodeAttributeTableModel attributes = ((NodeView) nodeView).getAttributeView().getAttributes();
				for (String attributeKey : attributes.getAttributeKeyList()) {
					AttributeController.getController(MModeController.getMModeController()).performRemoveRow(attributes,
							attributes.getAttributePosition(attributeKey));
				}
				if (attributes.getRowCount() <= 0) {
					((NodeView) nodeView).getAttributeView().viewRemoved();
				}
			}
		}
	}

	public static Object getAttributeValue(NodeModel target, String attributeKey) {
		if (target == null || attributeKey == null) return null;
		NodeAttributeTableModel attributes = AttributeController.getController(MModeController.getMModeController()).createAttributeTableModel(target);
		if (attributes != null) {
			if (attributes.getAttributeKeyList().contains(attributeKey)) {
				return attributes.getAttribute(attributes.getAttributePosition(attributeKey)).getValue();
			}
		}
		return null;
	}

	public static int getAttributeIntValue(NodeModel target, String attributeKey) {
		Object o = getAttributeValue(target, attributeKey);
		Integer value = 0;
		if (o == null) {
			return value;
		}
		if (o instanceof Integer) {
			value = (Integer) o;
		}
		else {
			try {
				value = Integer.parseInt(o.toString());
			}
			catch (NumberFormatException e) {
				LogUtils.severe("Could not read Attribute Key: " + attributeKey + " . Number expected.", e);
			}
		}
		return value;
	}

}
