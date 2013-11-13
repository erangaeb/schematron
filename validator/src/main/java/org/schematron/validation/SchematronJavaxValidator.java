package org.schematron.validation;

import org.apache.log4j.Logger;
import org.schematron.commons.XsltVersion;
import org.schematron.loader.SchematronLoader;
import org.schematron.model.Assertion;
import org.schematron.model.SchematronResult;
import org.schematron.model.SchematronResultImpl;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.validation.Validator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * SchematronValidator is a processor that checks an XML document against a schematron schema.
 * @author bard.langoy 
 */
public class SchematronJavaxValidator extends Validator {
    static Logger logger = Logger.getLogger(SchematronLoader.class);
    private ErrorHandler errorHandler;
    private LSResourceResolver resourceResolver;
    private SchematronTransformerImpl transformer;

    public SchematronJavaxValidator(InputSource inputSource, XsltVersion version, LSResourceResolver resolver, boolean compileSchematron) throws TransformerException, IOException, SAXException {
        this.resourceResolver = resolver;
        this.transformer = new SchematronTransformerImpl(inputSource, version, resolver, compileSchematron);
    }

    @Override
    public void reset() {
        logger.info("Method reset() called for SchematronValidator...");
    }

    @Override
    public void validate(Source source, Result result) throws SAXException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SchematronResult schematronResult = transformer.transform(source);

        insertSchematronResultToErrorHandler(schematronResult);

        if (!isValid(schematronResult)) {
            if (schematronResult instanceof SchematronResultImpl) {
                throw new SAXException(((SchematronResultImpl)schematronResult).toXml());
            } else {
                throw new SAXException(schematronResult.toString());
            }
        }
    }

    private void insertSchematronResultToErrorHandler(SchematronResult schematronResult) throws SAXException {
        for (Assertion assertion : schematronResult.getWarnings()) {
            getErrorHandler().warning(new SAXParseException(assertion.getDescription(), assertion.getName(), assertion.getName(), -1, -1));
        }
        for (Assertion assertion : schematronResult.getErrors()) {
            getErrorHandler().error(new SAXParseException(assertion.getDescription(), assertion.getName(), assertion.getName(), -1, -1));
        }
        for (Assertion assertion : schematronResult.getFatals()) {
            getErrorHandler().fatalError(new SAXParseException(assertion.getDescription(), assertion.getName(), assertion.getName(), -1, -1));
        }
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public void setResourceResolver(LSResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    @Override
    public LSResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public boolean isValid(SchematronResult result) {
        return result.getErrors().isEmpty() && result.getFatals().isEmpty();
    }
}
