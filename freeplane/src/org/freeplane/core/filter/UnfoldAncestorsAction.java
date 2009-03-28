/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2009 Dimitry Polivaev
 *
 *  This file author is Dimitry Polivaev
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.core.filter;

import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.freeplane.core.modecontroller.MapController;
import org.freeplane.core.modecontroller.ModeController;
import org.freeplane.core.model.NodeModel;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.AFreeplaneAction;

/**
 * @author Dimitry Polivaev
 * Mar 28, 2009
 */
class UnfoldAncestorsAction extends AFreeplaneAction {
	/**
     * 
     */
    private final FilterController filterController;

	/**
	 * @param filterController TODO
	 *
	 */
	UnfoldAncestorsAction(FilterController filterController) {
		super(filterController.getController(), null, new ImageIcon(ResourceController.getResourceController().getResource("/images/unfold.png")));
		this.filterController = filterController;
	}

	public void actionPerformed(final ActionEvent e) {
    	if (this.filterController.getSelectedCondition() != null) {
    		unfoldAncestors(this.filterController.getController().getMap().getRootNode());
    	}
    }

	private void setFolded(final NodeModel node, final boolean state) {
		final ModeController modeController = this.filterController.getController().getModeController();
		final MapController mapController = modeController.getMapController();
		if (mapController.hasChildren(node) && (mapController.isFolded(node) != state)) {
			mapController.setFolded(node, state);
		}
	}

	private void unfoldAncestors(final NodeModel parent) {
		for (final Iterator i = this.filterController.getController().getModeController().getMapController().childrenUnfolded(parent); i
		    .hasNext();) {
			final NodeModel node = (NodeModel) i.next();
			if (this.filterController.getShowDescendants().isSelected() || node.getFilterInfo().isAncestor()) {
				setFolded(node, false);
				unfoldAncestors(node);
			}
		}
	}
}