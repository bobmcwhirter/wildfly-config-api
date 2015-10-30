package org.wildfly.swarm.config.generator.generator;

import com.google.common.base.CaseFormat;
import org.jboss.dmr.ModelType;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.logmanager.Level;
import org.wildfly.swarm.config.generator.model.ResourceDescription;
import org.wildfly.swarm.config.runtime.Address;
import org.wildfly.swarm.config.runtime.Addresses;
import org.wildfly.swarm.config.runtime.Implicit;
import org.wildfly.swarm.config.runtime.ModelNodeBinding;
import org.wildfly.swarm.config.runtime.ResourceType;
import org.wildfly.swarm.config.runtime.Subresource;
import org.wildfly.swarm.config.runtime.invocation.Types;
import org.wildfly.swarm.config.runtime.model.AddressTemplate;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Encapsulates the templates for generating source files from resource descriptions
 *
 * @author Heiko Braun
 * @since 30/07/15
 */
public class ResourceClassFactory {

    private static final Logger log = Logger.getLogger(ResourceClassFactory.class.getName());

    /**
     * Base template for a resource representation.
     * Covers the resource attributes
     *
     * @param index
     * @param plan
     * @return
     */
    public static JavaClassSource createResourceAsClass(ClassIndex index, ClassPlan plan) {

        // base class
        JavaClassSource javaClass = Roaster.parse(
                JavaClassSource.class,
                "public class " + plan.getClassName() + "<T extends " + plan.getClassName() + "> {}"
        );

        // resource name
        javaClass.addField()
                .setName("key")
                .setPrivate()
                .setType(String.class);

        // resource references
        if(1==plan.getAddresses().size())
        {
            AnnotationSource<JavaClassSource> addressMeta = javaClass.addAnnotation(Address.class);
            addressMeta.setStringValue(plan.getAddresses().get(0).toString());
        }
        else {
            String[] addresses = new String[plan.getAddresses().size()];
            int i = 0;
            for (AddressTemplate addressTemplate : plan.getAddresses()) {
                addresses[i] = addressTemplate.toString();
                i++;
            }
            AnnotationSource<JavaClassSource> addressesMeta = javaClass.addAnnotation(Addresses.class);
            addressesMeta.setStringArrayValue(addresses);
        }

        // constructors
        boolean isSingleton = plan.isSingleton();
        if (isSingleton) {
            javaClass.addMethod()
                    .setConstructor(true)
                    .setPublic()
                    .setBody("this.key = \"" + plan.getSingletonName() + "\";\n"
                            + "this.pcs = new PropertyChangeSupport(this);");
        }
        else
        {
            // regular resources need to provide a key
            javaClass.addMethod()
                    .setConstructor(true)
                    .setPublic()
                    .setBody("this.key = key;")
                    .addParameter(String.class, "key");

        }

        javaClass.addMethod()
                .setName("getKey")
                .setPublic()
                .setReturnType(String.class)
                .setBody("return this.key;");


        javaClass.setPackage(plan.getPackageName());

        // javadoc
        JavaDocSource javaDoc = javaClass.getJavaDoc();
        ResourceDescription desc = plan.getDescription();
        javaDoc.setText(desc.getText());

        // imports
        javaClass.addImport(Implicit.class);
        javaClass.addImport(Address.class);
        javaClass.addImport(Addresses.class);
        javaClass.addImport(ResourceType.class);
        javaClass.addImport(ModelNodeBinding.class);
        javaClass.addImport(PropertyChangeListener.class);
        javaClass.addImport(PropertyChangeSupport.class);

        AnnotationSource<JavaClassSource> typeAnno = javaClass.addAnnotation();
        typeAnno.setName("ResourceType");
        typeAnno.setStringValue(plan.getResourceType());


        if (isSingleton) {
            AnnotationSource<JavaClassSource> implicitMeta = javaClass.addAnnotation();
            implicitMeta.setName("Implicit");
        }

           // property change listeners
        javaClass.addField()
                .setName("pcs")
                .setType(PropertyChangeSupport.class)
                .setPrivate();

        final MethodSource<JavaClassSource> listenerAdd = javaClass.addMethod();
        listenerAdd.getJavaDoc().setText("Adds a property change listener");
        listenerAdd.setPublic()
                .setName("addPropertyChangeListener")
                .addParameter(PropertyChangeListener.class, "listener");
        listenerAdd.setBody("if(null==this.pcs) this.pcs = new PropertyChangeSupport(this);\n" +
                "this.pcs.addPropertyChangeListener(listener);");

        final MethodSource<JavaClassSource> listenerRemove = javaClass.addMethod();
        listenerRemove.getJavaDoc().setText("Removes a property change listener");
        listenerRemove.setPublic()
                .setName("removePropertyChangeListener")
                .addParameter(PropertyChangeListener.class, "listener");
        listenerRemove.setBody("if(this.pcs!=null) this.pcs.removePropertyChangeListener(listener);");

        Inflector inflector = new Inflector();

        desc.getAttributes().forEach(
                att -> {
                    ModelType modelType = ModelType.valueOf(att.getValue().get(TYPE).asString());
                    Optional<String> resolvedType = Types.resolveJavaTypeName(modelType, att.getValue());

                    if (resolvedType.isPresent() && !att.getValue().get(DEPRECATED).isDefined()) {

                        // attributes
                        try {
                            final String name = javaAttributeName(att.getName());
                            String attributeDescription = att.getValue().get(DESCRIPTION).asString();

                            FieldSource attributeField = javaClass.addField()
                                    .setName(name)
                                    .setType(resolvedType.get())
                                    .setPrivate();

                            final MethodSource<JavaClassSource> accessor = javaClass.addMethod();
                            accessor.getJavaDoc().setText(attributeDescription);
                            accessor.setPublic()
                                    .setName(name)
                                    .setReturnType(resolvedType.get())
                                    .setBody("return this." + name + ";");


                            final MethodSource<JavaClassSource> mutator = javaClass.addMethod();
                            mutator.getJavaDoc().setText(attributeDescription);
                            mutator.addParameter(resolvedType.get(), "value");
                            mutator.setPublic()
                                    .setName(name)
                                    .setReturnType("T")
                                    .setBody("Object oldValue = this."+name+";\n"+
                                            "this." + name + " = value;\n" +
                                            "if(this.pcs!=null) this.pcs.firePropertyChange(\""+name+"\", oldValue, value);\n" +
                                            "return (T) this;")
                                    .addAnnotation("SuppressWarnings").setStringValue("unchecked");

                            AnnotationSource<JavaClassSource> bindingMeta = accessor.addAnnotation();
                            bindingMeta.setName("ModelNodeBinding");
                            bindingMeta.setStringValue("detypedName", att.getName());

                            // If the model type is LIST, then also add an appending mutator
                            if (modelType == ModelType.LIST) {
                                String singularName = inflector.singularize( name );
                                // initialize the field to an array list
                                //attributeField.setLiteralInitializer("new java.util.ArrayList<>()");
                                final MethodSource<JavaClassSource> appender = javaClass.addMethod();
                                appender.getJavaDoc().setText(attributeDescription);
                                appender.addParameter(Types.resolveValueType(att.getValue()), "value");
                                appender.setPublic()
                                        .setName(singularName) // non-trivial to singularize the method name here
                                        .setReturnType("T")
                                        .setBody(" if ( this."+name + " == null ) { this." + name + " = new java.util.ArrayList<>(); }\nthis." + name + ".add(value);\nreturn (T) this;");
                            } else if (modelType == ModelType.OBJECT) {
                                // initialize the field to a HashMap
                                //attributeField.setLiteralInitializer("new java.util.HashMap<String, Object>()");
                                String singularName = inflector.singularize( name );
                                final MethodSource<JavaClassSource> appender = javaClass.addMethod();
                                appender.getJavaDoc().setText(attributeDescription);
                                appender.addParameter(String.class, "key");
                                appender.addParameter(Object.class, "value");
                                appender.setPublic()
                                        .setName(singularName)
                                        .setReturnType("T")
                                        .setBody(" if ( this." + name + " == null ) { this." + name + " = new java.util.HashMap<>(); }\nthis." + name + ".put(key, value);\nreturn (T) this;");
                            }
                        } catch (Exception e) {
                            log.log(Level.ERROR, "Failed to process " + plan.getFullyQualifiedClassName() + ", attribute " + att.getName(), e);
                        }
                    } //else System.err.println(att.getValue());
                }
        );

        if ( ! desc.getChildrenTypes().isEmpty() ) {
            createChildAccessors( index, plan, javaClass );
        }

        if ( ! desc.getSingletonChildrenTypes().isEmpty() ) {
            createSingletonChildAccessors( index, plan, javaClass );
        }

        plan.setResourceClassSource(javaClass);

        return javaClass;
    }

    /**
     * Decorates a base resource representation with accessors to it's child resources
     *
     * @param index
     * @param plan
     * @param javaClass
     */
    public static void createChildAccessors(ClassIndex index, ClassPlan plan, JavaClassSource javaClass) {

        ResourceMetaData resourceMetaData = plan.getMetaData();

        final JavaClassSource subresourceClass = createSubresourceClass(plan, javaClass);

        // For each subresource create a getter/mutator/list-mutator
        final ResourceDescription resourceMetaDataDescription = resourceMetaData.getDescription();
        final Set<String> childrenNames = resourceMetaDataDescription.getChildrenTypes();
        for (String childName : childrenNames) {

            final AddressTemplate childAddress = resourceMetaData.getAddress().append(childName + "=*");
            final ClassPlan childClass = index.lookup(childAddress);
            //javaClass.addImport(childClass);

            final String childClassName = childClass.getFullyQualifiedClassName();
            final String propType = "java.util.List<" + childClassName + ">";
            String propName = CaseFormat.UPPER_CAMEL.to(
                    CaseFormat.LOWER_CAMEL,
                    Keywords.escape(childClass.getOriginalClassName())
            );
            String singularName = propName;
            if (!propName.endsWith("s")) {
                propName += "s";
            }

            // Add a property and an initializer for this subresource to the class
            final String resourceText = resourceMetaDataDescription.getChildDescription(childName).getText();
            subresourceClass.addField()
                    .setName(propName)
                    .setType(propType)
                    .setPrivate()
                    .setLiteralInitializer("new java.util.ArrayList<>();")
                    .getJavaDoc().setText(resourceText);

            // Add an accessor method
            final MethodSource<JavaClassSource> accessor = subresourceClass.addMethod();
            accessor.getJavaDoc()
                    .setText("Get the list of " + childClassName + " resources")
                    .addTagValue("@return", "the list of resources");
            accessor.setPublic()
                    .setName(propName)
                    .setReturnType(propType)
                    .setBody("return this." + propName + ";");

            // Add a mutator method that takes a list of resources. Mutators are added to the containing class
            final MethodSource<JavaClassSource> listMutator = javaClass.addMethod();
            listMutator.getJavaDoc()
                    .setText("Add all " + childClassName + " objects to this subresource")
                    .addTagValue("@return", "this")
                    .addTagValue("@param", "value List of " + childClassName + " objects.");
            listMutator.addParameter(propType, "value");
            listMutator.setPublic()
                    .setName(propName)
                    .setReturnType("T")
                    .setBody("this.subresources." + propName + " = value;\nreturn (T) this;")
                    .addAnnotation("SuppressWarnings").setStringValue("unchecked");

            // Add a mutator method that takes a single resource. Mutators are added to the containing class
            final MethodSource<JavaClassSource> mutator = javaClass.addMethod();
            mutator.getJavaDoc()
                    .setText("Add the " + childClassName + " object to the list of subresources")
                    .addTagValue("@param", "value The " + childClassName + " to add")
                    .addTagValue("@return", "this");
            mutator.addParameter(childClassName, "value");
            mutator.setPublic()
                    .setName(singularName)
                    .setReturnType( "T" )
                    .setBody("this.subresources." + propName + ".add(value);\nreturn (T) this;")
                    .addAnnotation("SuppressWarnings").setStringValue("unchecked");

            // Add a mutator method that factories a single resource and applies a supplied configurator. Mutators are added to the containing class
            final MethodSource<JavaClassSource> configurator = javaClass.addMethod();
            configurator.getJavaDoc()
                    .setText("Create and configure a " + childClassName + " object to the list of subresources")
                    .addTagValue("@param", "key The key for the " + childClassName + " resource" )
                    .addTagValue("@param", "config The " + childClassName + "Configurator to use")
                    .addTagValue("@return", "this");
            configurator.addParameter(String.class, "childKey");
            configurator.addParameter(childClassName + "Configurator", "config");
            configurator.setPublic()
                    .setName(singularName)
                    .setReturnType("T")
                    .setBody( childClassName + " child = new " + childClassName + "(childKey);\n if ( config != null ) { config.configure(child); }\n" + singularName +"(child);\nreturn (T) this;")
                    .addAnnotation("SuppressWarnings").setStringValue("unchecked");

            // Add a mutator method that factories a single resource and applies a supplied configurator. Mutators are added to the containing class
            final MethodSource<JavaClassSource> nonConfigurator = javaClass.addMethod();
            nonConfigurator.getJavaDoc()
                    .setText("Create and configure a " + childClassName + " object to the list of subresources")
                    .addTagValue("@param", "key The key for the " + childClassName + " resource" )
                    .addTagValue("@return", "this");
            nonConfigurator.addParameter(String.class, "childKey");
            nonConfigurator.setPublic()
                    .setName(singularName)
                    .setReturnType("T")
                    .setBody( singularName + "(childKey, null);\nreturn (T) this;\n")
                    .addAnnotation("SuppressWarnings").setStringValue("unchecked");

            final AnnotationSource<JavaClassSource> subresourceMeta = accessor.addAnnotation();
            subresourceMeta.setName("Subresource");

        }

        // initialize the collections
        javaClass.addNestedType(subresourceClass);
    }

    private static JavaClassSource createSubresourceClass(ClassPlan plan, JavaClassSource javaClass) {

        JavaClassSource subresourceClass = Roaster.parse(
                JavaClassSource.class,
                "class " + javaClass.getName() + "Resources" + " {}"
        );
        subresourceClass.setPackage(plan.getPackageName());
        subresourceClass.getJavaDoc().setText("Child mutators for " + javaClass.getName());
        subresourceClass.setPublic();

        javaClass.addField()
                .setPrivate()
                .setType(subresourceClass.getName())
                .setName("subresources")
                .setLiteralInitializer("new " + subresourceClass.getName() + "();");

        final MethodSource<JavaClassSource> subresourcesMethod = javaClass.addMethod()
                .setName("subresources")
                .setPublic();
        subresourcesMethod.setReturnType(subresourceClass.getName());
        subresourcesMethod.setBody("return this.subresources;");

        javaClass.addImport("java.util.List");
        javaClass.addImport(Subresource.class);
        return subresourceClass;
    }

    public static void createSingletonChildAccessors(ClassIndex index, ClassPlan plan, JavaClassSource javaClass) {

        ResourceMetaData resourceMetaData = plan.getMetaData();

        final ResourceDescription description = resourceMetaData.getDescription();
        final Set<String> singletonNames = description.getSingletonChildrenTypes();
        javaClass.addImport(Subresource.class);
        for (String singletonName : singletonNames) {

            String[] split = singletonName.split("=");
            String type = split[0];
            String name = split[1];
            final AddressTemplate childAddress = resourceMetaData.getAddress().append(type + "=" + name);
            final ClassPlan childClass = index.lookup(childAddress);
            //javaClass.addImport(childClass);

            String propName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, childClass.getOriginalClassName());

            javaClass.addField()
                    .setName(propName)
                    .setType(childClass.getFullyQualifiedClassName())
                    .setPrivate();

            // Add an accessor method
            final MethodSource<JavaClassSource> accessor = javaClass.addMethod();
            String javaDoc = description.getChildDescription(type, name).getText();
            accessor.getJavaDoc()
                    .setText(javaDoc);
            accessor.setPublic()
                    .setName(propName)
                    .setReturnType(childClass.getFullyQualifiedClassName())
                    .setBody("return this." + propName + ";");

            AnnotationSource<JavaClassSource> subresourceMeta = accessor.addAnnotation();
            subresourceMeta.setName("Subresource");

            // Add a mutator
            final MethodSource<JavaClassSource> mutator = javaClass.addMethod();
            mutator.getJavaDoc()
                    .setText(javaDoc);
            mutator.addParameter(childClass.getFullyQualifiedClassName(), "value");
            mutator.setPublic()
                    .setName(propName)
                    .setReturnType("T")
                    .setBody("this." + propName + "=value;\nreturn (T) this;")
                    .addAnnotation("SuppressWarnings").setStringValue("unchecked");

        }
    }

    public final static String javaAttributeName(String dmr) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, Keywords.escape(dmr.replace("-", "_")));
    }
}