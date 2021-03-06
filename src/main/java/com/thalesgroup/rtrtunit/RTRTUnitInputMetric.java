package com.thalesgroup.rtrtunit;

import hudson.model.Hudson;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.jenkinsci.lib.dtkit.model.InputMetricOther;
import org.jenkinsci.lib.dtkit.model.InputType;
import org.jenkinsci.lib.dtkit.model.OutputMetric;
import org.jenkinsci.lib.dtkit.util.converter.ConversionException;
import org.jenkinsci.lib.dtkit.util.validator.ValidationError;
import org.jenkinsci.lib.dtkit.util.validator.ValidationException;
import org.jenkinsci.lib.dtkit.util.validator.ValidationService;
import org.jenkinsci.plugins.xunit.types.model.JUnitModel;

import com.thalesgroup.rtrtunit.converter.RTRTtoXMLConverter;
import com.thalesgroup.rtrtunit.rioreader.RioReader;
import com.thalesgroup.rtrtunit.tdcreader.TdcException;

/**
 * RTRTUnitInputMetric.
 *
 * @author Sebastien Barbier
 * @version 1.0
 */
public class RTRTUnitInputMetric extends InputMetricOther {

	private static final long serialVersionUID = -1339080252033826104L;

	/**
     * Version du plugin.
     */
    private String version = null;

    /**
     * Get InputType.
     *
     * @return InputType.TEST
     */
    @Override
    public final InputType getToolType() {
        return InputType.TEST;
    }

    /**
     * Return Name of the plugin.
     *
     * @return name of the plugin
     */
    @Override
    public final String getToolName() {
        return "RTRTUnit";
    }

    /**
     * Return version.
     *
     * @return version of the plugin
     */
    @Override
    public final String getToolVersion() {
        if (version == null) {
        	if (Hudson.getInstance()!=null){
        		version = Hudson.getInstance().getPluginManager().getPlugin("rtrtunit").getVersion();
        	}
        	else {
        		version = "N/A";
        	}
        }
        return version;
    }

    /**
     * Conversion of the input toward the output.
     *
     * @param inputRIOFile
     *            the .rio file
     * @param outXMLFile
     *            the junit xml report
     * @param map
     *            useless
     */
    @Override
    public final void convert(final File inputRIOFile, final File outXMLFile,
            final Map<String, Object> map) {
        RTRTtoXMLConverter converter = null;
        try {
            converter = new RTRTtoXMLConverter(inputRIOFile, outXMLFile);
        } catch (TdcException e) {
            throw new ConversionException(e.getMessage());
        } catch (IOException e) {
            throw new ConversionException(e.getMessage());
        }
        converter.buildHeader();
        try {
            converter.buildTests();
        } catch (TdcException e) {
            throw new ConversionException(e.getMessage());
        }
        converter.writeXML();
    }

    /**
     * Validation of the .rio file according to the RioGrammar.jj. Skip if an
     * .err file exists.
     *
     * @param inputFile
     *            the .rio file
     * @return true if the file is correct
     * @see RioReader
     */
    @Override
    public final boolean validateInputFile(final File inputFile) {
        String nameTest = inputFile.getAbsolutePath().substring(0,
                inputFile.getAbsolutePath().lastIndexOf('.'));
        File errFile = new File(nameTest + ".err");
        if (errFile.exists()) {
            return true;
        }
		// check thath TDC file exists
		File inputTdcFile = new File(inputFile.getAbsolutePath().substring(0,
				inputFile.getAbsolutePath().lastIndexOf('.'))
                + ".tdc");
		return inputTdcFile.exists() && new RioReader().validate(inputFile);
    }

    /**
     * Validation of the junit report xml file according to the junit.xsd.
     *
     * @param inputXMLFile
     *            : the junit report xml file generated by our converter
     * @return true if the file is correct Based on the .xsd file
     */
    @Override
    public final boolean validateOutputFile(final File inputXMLFile) {
        // Give the .xsd file included into the resource path.
        Source source = new StreamSource(
        		this.getClass().getResourceAsStream("xsd/junit-1.0.xsd"));

        // Apply the processValidation already encoded into the dtkit and check
        // if some errors.
        List<ValidationError> lve = new ValidationService().processValidation(
                source, inputXMLFile);
        for (ValidationError ve : lve) {
            throw new ValidationException(ve.toString());
        }

        // No error => validation is ok!
        return lve.isEmpty();
    }

    /**
     * Get OutputType.
     *
     * @return OutputType.JUNIT_1
     */
    @Override
    public final OutputMetric getOutputFormatType() {
        return JUnitModel.LATEST;
    }
}
