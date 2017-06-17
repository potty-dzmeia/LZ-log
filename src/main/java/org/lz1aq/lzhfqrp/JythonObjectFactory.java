package org.lz1aq.lzhfqrp;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PySystemState;

/**
 * Jython Object Factory using PySystemState
 */
public class JythonObjectFactory
{

    private final Class interfaceType;
    private final PyObject klass;

    // 
    /**
     * Constructor obtains a reference to the importer, module, and the class name
     * 
     * @param state
     * @param interfaceType
     * @param moduleName
     * @param className 
     */
    public JythonObjectFactory(PySystemState state, Class interfaceType, String moduleName, String className)
    {
        this.interfaceType = interfaceType;
        
        PyObject fnImporter = state.getBuiltins().__getitem__(Py.newString("__import__"));
        PyObject module = fnImporter.__call__(Py.newString(moduleName)); // The function imports the module name, 
        klass = module.__getattr__(className);                           // This method should return the (computed) attribute 
        System.err.println("module=" + module + ",class=" + klass);
    }

    // 
    /**
     * Constructor obtains a reference to the __importer__, module, and the class name
     * 
     * @param interfaceType - Type of classes that will be created
     * @param moduleName - module name containing the python class
     * @param className - name of the python class
     */
    public JythonObjectFactory(Class interfaceType, String moduleName, String className)
    {
        this(new PySystemState(), interfaceType, moduleName, className);
    }

    // All of the followng methods return
    // a coerced Jython object based upon the pieces of information
    // that were passed into the factory. The differences are
    // between them are the number of arguments that can be passed
    // in as arguents to the object.
    public Object createObject()
    {
        return klass.__call__().__tojava__(interfaceType);
    }

    public Object createObject(Object arg1)
    {
        return klass.__call__(Py.java2py(arg1)).__tojava__(interfaceType);
    }

    public Object createObject(Object arg1, Object arg2)
    {
        return klass.__call__(Py.java2py(arg1), Py.java2py(arg2)).__tojava__(interfaceType);
    }

    public Object createObject(Object arg1, Object arg2, Object arg3)
    {
        return klass.__call__(Py.java2py(arg1), Py.java2py(arg2),
                Py.java2py(arg3)).__tojava__(interfaceType);
    }

    public Object createObject(Object args[], String keywords[])
    {
        PyObject convertedArgs[] = new PyObject[args.length];
        for (int i = 0; i < args.length; i++)
        {
            convertedArgs[i] = Py.java2py(args[i]);
        }

        return klass.__call__(convertedArgs, keywords).__tojava__(interfaceType);
    }

    public Object createObject(Object... args)
    {
        return createObject(args, Py.NoKeywords);
    }

}
