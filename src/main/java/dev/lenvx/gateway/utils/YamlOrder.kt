package dev.lenvx.gateway.utils

import org.yaml.snakeyaml.error.YAMLException
import org.yaml.snakeyaml.introspector.BeanAccess
import org.yaml.snakeyaml.introspector.FieldProperty
import org.yaml.snakeyaml.introspector.MethodProperty
import org.yaml.snakeyaml.introspector.MissingProperty
import org.yaml.snakeyaml.introspector.Property
import org.yaml.snakeyaml.introspector.PropertyUtils
import org.yaml.snakeyaml.util.PlatformFeatureDetector
import java.beans.FeatureDescriptor
import java.beans.IntrospectionException
import java.beans.Introspector
import java.lang.reflect.Modifier
import java.util.LinkedHashMap
import java.util.LinkedHashSet

class YamlOrder(
    private val platformFeatureDetector: PlatformFeatureDetector = PlatformFeatureDetector()
) : PropertyUtils() {

    private val propertiesCache: MutableMap<Class<*>, MutableMap<String, Property>> = HashMap()
    private val readableProperties: MutableMap<Class<*>, MutableSet<Property>> = HashMap()
    private var beanAccess: BeanAccess = BeanAccess.DEFAULT
    private var allowReadOnlyProperties: Boolean = false
    private var skipMissingProperties: Boolean = false

    init {
        if (platformFeatureDetector.isRunningOnAndroid) {
            beanAccess = BeanAccess.FIELD
        }
    }

    override fun getPropertiesMap(type: Class<*>, bAccess: BeanAccess): MutableMap<String, Property> {
        val cached = propertiesCache[type]
        if (cached != null) {
            return cached
        }

        val properties: MutableMap<String, Property> = LinkedHashMap()
        var inaccessibleFieldsExist = false

        when (bAccess) {
            BeanAccess.FIELD -> {
                var c: Class<*>? = type
                while (c != null) {
                    for (field in c.declaredFields) {
                        val modifiers = field.modifiers
                        if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers) && !properties.containsKey(field.name)) {
                            properties[field.name] = FieldProperty(field)
                        }
                    }
                    c = c.superclass
                }
            }
            else -> {
                try {
                    for (property in Introspector.getBeanInfo(type).propertyDescriptors) {
                        val readMethod = property.readMethod
                        if ((readMethod == null || readMethod.name != "getClass") && !isTransient(property)) {
                            properties[property.name] = MethodProperty(property)
                        }
                    }
                } catch (e: IntrospectionException) {
                    throw YAMLException(e)
                }

                var c: Class<*>? = type
                while (c != null) {
                    for (field in c.declaredFields) {
                        val modifiers = field.modifiers
                        if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                            if (Modifier.isPublic(modifiers)) {
                                properties[field.name] = FieldProperty(field)
                            } else {
                                inaccessibleFieldsExist = true
                            }
                        }
                    }
                    c = c.superclass
                }
            }
        }

        if (properties.isEmpty() && inaccessibleFieldsExist) {
            throw YAMLException("No JavaBean properties found in ${type.name}")
        }

        propertiesCache[type] = properties
        return properties
    }

    private fun isTransient(fd: FeatureDescriptor): Boolean {
        return java.lang.Boolean.TRUE == fd.getValue(TRANSIENT)
    }

    override fun getProperties(type: Class<out Any>): MutableSet<Property> {
        return getProperties(type, beanAccess)
    }

    override fun getProperties(type: Class<out Any>, bAccess: BeanAccess): MutableSet<Property> {
        val cached = readableProperties[type]
        if (cached != null) {
            return cached
        }
        val properties = createPropertySet(type, bAccess)
        readableProperties[type] = properties
        return properties
    }

    override fun createPropertySet(type: Class<out Any>, bAccess: BeanAccess): MutableSet<Property> {
        val properties: MutableSet<Property> = LinkedHashSet()
        val props = getPropertiesMap(type, bAccess).values
        for (property in props) {
            if (property.isReadable && (allowReadOnlyProperties || property.isWritable)) {
                properties.add(property)
            }
        }
        return properties
    }

    override fun getProperty(type: Class<out Any>, name: String): Property {
        return getProperty(type, name, beanAccess)
    }

    override fun getProperty(type: Class<out Any>, name: String, bAccess: BeanAccess): Property {
        val properties = getPropertiesMap(type, bAccess)
        var property: Property? = properties[name]
        if (property == null && skipMissingProperties) {
            property = MissingProperty(name)
        }
        if (property == null) {
            throw YAMLException("Unable to find property '$name' on class: ${type.name}")
        }
        return property
    }

    override fun setBeanAccess(beanAccess: BeanAccess) {
        if (platformFeatureDetector.isRunningOnAndroid && beanAccess != BeanAccess.FIELD) {
            throw IllegalArgumentException("JVM is Android - only BeanAccess.FIELD is available")
        }

        if (this.beanAccess != beanAccess) {
            this.beanAccess = beanAccess
            propertiesCache.clear()
            readableProperties.clear()
        }
    }

    override fun setAllowReadOnlyProperties(allowReadOnlyProperties: Boolean) {
        if (this.allowReadOnlyProperties != allowReadOnlyProperties) {
            this.allowReadOnlyProperties = allowReadOnlyProperties
            readableProperties.clear()
        }
    }

    override fun isAllowReadOnlyProperties(): Boolean {
        return allowReadOnlyProperties
    }

    override fun setSkipMissingProperties(skipMissingProperties: Boolean) {
        if (this.skipMissingProperties != skipMissingProperties) {
            this.skipMissingProperties = skipMissingProperties
            readableProperties.clear()
        }
    }

    override fun isSkipMissingProperties(): Boolean {
        return skipMissingProperties
    }

    companion object {
        private const val TRANSIENT = "transient"
    }
}
