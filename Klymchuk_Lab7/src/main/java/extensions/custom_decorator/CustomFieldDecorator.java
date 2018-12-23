package extensions.custom_decorator;

import elements.Element;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.pagefactory.DefaultElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.DefaultFieldDecorator;
import org.openqa.selenium.support.pagefactory.ElementLocator;

import java.lang.reflect.*;
import java.util.List;

public class CustomFieldDecorator extends DefaultFieldDecorator{
    public CustomFieldDecorator(SearchContext searchContext) {
        super(new DefaultElementLocatorFactory(searchContext));
    }

    @Override
    public Object decorate(ClassLoader loader, Field field) {
        Class<Element> decoratableClass = decoratableClass(field);
        if (decoratableClass != null) {
            ElementLocator locator = factory.createLocator(field);
            if (locator == null)
                return null;
            if (List.class.isAssignableFrom(field.getType()))
                return createList(loader, locator, decoratableClass);
            return createElement(loader, locator, decoratableClass);
        }
        return super.decorate(loader, field);
    }

    @SuppressWarnings("unchecked")
    private Class<Element> decoratableClass(Field field) {
        Class<?> clazz = field.getType();
        if (List.class.isAssignableFrom(clazz)) {
            if (field.getAnnotation(FindBy.class) == null && field.getAnnotation(FindBys.class) == null)
                return null;
            Type genericType = field.getGenericType();
            if (!(genericType instanceof ParameterizedType))
                return null;
            clazz = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
        }
        if (Element.class.isAssignableFrom(clazz))
            return (Class<Element>) clazz;
        else
            return null;
    }

    protected Element createElement(ClassLoader loader, ElementLocator locator, Class<Element> clazz) {
        WebElement proxy = proxyForLocator(loader, locator);
        return WrapperFactory.createInstance(clazz, proxy);
    }

    @SuppressWarnings("unchecked")
    protected List<Element> createList(ClassLoader loader, ElementLocator locator, Class<Element> clazz) {
        InvocationHandler handler = new LocatingCustomElementListHandler(locator, clazz);
        List<Element> elements = (List<Element>) Proxy.newProxyInstance(loader, new Class[] {List.class}, handler);
        return elements;
    }
}
