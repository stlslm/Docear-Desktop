/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
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
package org.freeplane.features.mindmapmode.addins.export;

import java.awt.event.ActionEvent;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.freeplane.core.Compat;
import org.freeplane.core.controller.Controller;
import org.freeplane.core.io.MapWriter.Mode;
import org.freeplane.core.modecontroller.ModeController;
import org.freeplane.core.model.MapModel;
import org.freeplane.core.model.MindIcon;
import org.freeplane.core.resources.FreeplaneResourceBundle;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.core.util.LogTool;
import org.freeplane.core.util.ResUtil;
import org.freeplane.features.mindmapmode.MModeController;
import org.freeplane.n3.nanoxml.XMLElement;
import org.freeplane.n3.nanoxml.IXMLParser;
import org.freeplane.n3.nanoxml.IXMLReader;
import org.freeplane.n3.nanoxml.StdXMLReader;
import org.freeplane.n3.nanoxml.XMLParserFactory;

/**
 * @author foltin To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ExportWithXSLT extends ExportAction {
	private static final String NAME_EXTENSION_PROPERTY = "name_extension";

	/**
	 */
	private static void copyIconsToDirectory(final String directoryName2) {
		final List iconNames = MindIcon.getAllIconNames();
		for (int i = 0; i < iconNames.size(); ++i) {
			final String iconName = ((String) iconNames.get(i));
			final MindIcon myIcon = MindIcon.factory(iconName);
			ResUtil.copyFromResource(MindIcon.getIconsPath(), myIcon.getIconBaseFileName(), directoryName2);
		}
		final File iconDir = new File(ResourceController.getResourceController().getFreeplaneUserDirectory(), "icons");
		if (iconDir.exists()) {
			final String[] userIconArray = iconDir.list(new FilenameFilter() {
				public boolean accept(final File dir, final String name) {
					return name.matches(".*\\.png");
				}
			});
			for (int i = 0; i < userIconArray.length; ++i) {
				final String iconName = userIconArray[i];
				if (iconName.length() == 4) {
					continue;
				}
				ResUtil.copyFromFile(iconDir.getAbsolutePath(), iconName, directoryName2);
			}
		}
	}

	public static void createXSLTExportActions(final ModeController modeController, final String xmlDescriptorFile) {
		try {
			final IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
			final URL resource = ResourceController.getResourceController().getResource(xmlDescriptorFile);
			final IXMLReader reader = new StdXMLReader(resource.openStream());
			parser.setReader(reader);
			final XMLElement xml = (XMLElement) parser.parse();
			final Enumeration actionDescriptors = xml.enumerateChildren();
			while (actionDescriptors.hasMoreElements()) {
				final XMLElement descriptor = (XMLElement) actionDescriptors.nextElement();
				final String name = descriptor.getAttribute("name", null);
				final String tooltip = descriptor.getAttribute("tooltip", null);
				final String location = descriptor.getAttribute("location", null);
				final XMLElement xmlProperties = descriptor.getFirstChildNamed("properties");
				final Properties properties = xmlProperties.getAttributes();
				final ExportWithXSLT action = new ExportWithXSLT(modeController.getController(), name, tooltip,
				    properties);
				modeController.addAction(name, action);
				modeController.getUserInputListenerFactory().getMenuBuilder().addAction(location, action,
				    location + "/" + name, MenuBuilder.AS_CHILD);
			}
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * For test purposes. True=no error
	 */
	private boolean mTransformResultWithoutError = false;
	final private Properties properties;

	public ExportWithXSLT(final Controller controller, final String name, final String tooltip,
	                      final Properties properties) {
		super(controller, name);
		this.properties = properties;
		setTooltip(tooltip);
	}

	public void actionPerformed(final ActionEvent e) {
		final ModeController mc = getModeController();
		final MapModel model = getController().getMap();
		if (StringUtils.equals(getProperty("file_type"), "user")) {
			if (model == null) {
				return;
			}
			if ((model.getFile() == null) || model.isReadOnly()) {
				if (((MModeController) mc).save()) {
					export(model.getFile());
					return;
				}
				else {
					return;
				}
			}
			else {
				export(model.getFile());
			}
		}
		else {
			final File saveFile = chooseFile();
			if (saveFile == null) {
				return;
			}
			transform(saveFile);
		}
	}

	protected File chooseFile() {
		final String nameExtension = getProperty(ExportWithXSLT.NAME_EXTENSION_PROPERTY);
		return chooseFile(getProperty("file_type"), getTranslatableResourceString("file_description"), nameExtension);
	}

	/**
	 */
	private void copyFilesFromResourcesToDirectory(final String directoryName, final String files,
	                                               final String filePrefix) {
		final StringTokenizer tokenizer = new StringTokenizer(files, ",");
		while (tokenizer.hasMoreTokens()) {
			final String next = tokenizer.nextToken();
			ResUtil.copyFromResource(filePrefix, next, directoryName);
		}
	}

	/**
	 */
	private boolean copyIcons(final String directoryName) {
		boolean success;
		final String iconDirectoryName = directoryName + File.separatorChar + "icons";
		success = ResUtil.createDirectory(iconDirectoryName);
		if (success) {
			ExportWithXSLT.copyIconsToDirectory(iconDirectoryName);
		}
		return success;
	}

	private boolean copyMap(final String pDirectoryName) throws IOException {
		final boolean success = true;
		final BufferedWriter fileout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pDirectoryName
		        + File.separator + "map.mm")));
		final MapModel map = getController().getMap();
		getModeController().getMapController().getFilteredXml(map, fileout, Mode.FILE);
		return success;
	}

	/**
	 */
	private void createImageFromMap(final String directoryName) {
		if (getController().getViewController().getMapView() == null) {
			return;
		}
		final RenderedImage image = createBufferedImage();
		try {
			final FileOutputStream out = new FileOutputStream(directoryName + File.separator + "image.png");
			ImageIO.write(image, "png", out);
			out.close();
		}
		catch (final IOException e1) {
			LogTool.logException(e1);
		}
	}

	private void export(final File file) {
		final ExportDialog exp = new ExportDialog(file);
		exp.export();
	}

	/**
	 * @param create_image
	 */
	private String getAreaCode(final boolean create_image) {
		String areaCode = "";
		if (create_image) {
			areaCode = getController().getMapViewManager().createHtmlMap();
		}
		return areaCode;
	}

	/**
	 * @param mode 
	 * @throws IOException
	 */
	private String getMapXml(Mode mode) throws IOException {
		final StringWriter writer = new StringWriter();
		final ModeController modeController = getModeController();
		final Controller controller = modeController.getController();
		final MapModel map = controller.getMap();
		modeController.getMapController().getFilteredXml(map, writer, mode);
		return writer.getBuffer().toString();
	}

	String getProperty(final String key) {
		return getProperty(key, null);
	}

	String getProperty(final String key, final String value) {
		return properties.getProperty(key, value);
	}

	private String getTranslatableResourceString(final String resourceName) {
		final String returnValue = getProperty(resourceName);
		if (returnValue != null && returnValue.startsWith("%")) {
			return FreeplaneResourceBundle.getText(returnValue.substring(1));
		}
		return returnValue;
	}

	public boolean isTransformResultWithoutError() {
		return mTransformResultWithoutError;
	}

	/**
	 * @param saveFile
	 */
	protected void transform(final File saveFile) {
		try {
			mTransformResultWithoutError = true;
			final boolean create_image = StringUtils.equals(getProperty("create_html_linked_image"), "true");
			final String areaCode = getAreaCode(create_image);
			final String xsltFileName = getProperty("xslt_file");
			boolean success = transformMapWithXslt(xsltFileName, saveFile, areaCode);
			if (!success) {
				JOptionPane.showMessageDialog(null, getProperty("error_applying_template"), "Freeplane",
				    JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (success && StringUtils.equals(getProperty("create_dir"), "true")) {
				final String directoryName = saveFile.getAbsolutePath() + "_files";
				success = ResUtil.createDirectory(directoryName);
				if (success) {
					final String files = getProperty("files_to_copy");
					final String filePrefix = getProperty("file_prefix");
					copyFilesFromResourcesToDirectory(directoryName, files, filePrefix);
				}
				if (success && StringUtils.equals(getProperty("copy_icons"), "true")) {
					success = copyIcons(directoryName);
				}
				if (success && StringUtils.equals(getProperty("copy_map"), "true")) {
					success = copyMap(directoryName);
				}
				if (success && create_image) {
					createImageFromMap(directoryName);
				}
			}
			if (!success) {
				JOptionPane.showMessageDialog(null, getProperty("error_creating_directory"), "Freeplane",
				    JOptionPane.ERROR_MESSAGE);
				return;
			}
			if (StringUtils.equals(getProperty("load_file"), "true")) {
				getController().getViewController().openDocument(Compat.fileToUrl(saveFile));
			}
		}
		catch (final Exception e) {
			LogTool.logException(e);
			mTransformResultWithoutError = false;
		}
	}

	/**
	 * @throws IOException
	 */
	private boolean transformMapWithXslt(final String xsltFileName, final File saveFile, final String areaCode)
	        throws IOException {
		Mode mode = Mode.parse(getProperty("mode", "SAVE"));
		final String map = getMapXml(mode);
		final StringReader reader = new StringReader(map);
		final URL xsltUrl = ResourceController.getResourceController().getResource(xsltFileName);
		if (xsltUrl == null) {
			Logger.global.severe("Can't find " + xsltFileName + " as resource.");
			throw new IllegalArgumentException("Can't find " + xsltFileName + " as resource.");
		}
		final InputStream xsltFile = xsltUrl.openStream();
		final Source xsltSource = new StreamSource(xsltFile);
        final Result result = new StreamResult(saveFile);
        try {
        	final TransformerFactory transFact = TransformerFactory.newInstance();
        	final Transformer trans = transFact.newTransformer(xsltSource);
        	trans.setParameter("destination_dir", saveFile.getName() + "_files/");
        	trans.setParameter("area_code", areaCode);
        	trans.setParameter("folding_type", ResourceController.getResourceController().getProperty(
        	    "html_export_folding"));
        	trans.transform(new StreamSource(reader), result);
        }
        catch (final Exception e) {
        	LogTool.logException(e);
        	return false;
        };
        return true;
	}
}
