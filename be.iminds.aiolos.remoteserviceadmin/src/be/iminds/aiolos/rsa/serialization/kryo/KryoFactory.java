package be.iminds.aiolos.rsa.serialization.kryo;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;

import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

/**
 * This is a dirty workaround to have a Kryo instance to which we can easily
 * add/remove Kryo serializers from services ... could be solved more elegantly
 * by actually using the OSGi service mechanism for Serializers/Deserializers and 
 * NetworkChannelFactory etc.
 */
public class KryoFactory {

	private static Map<String, Serializer> serializers = new HashMap<String, Serializer>();
	private static Map<String, Integer> ids = new HashMap<String, Integer>();
	
	// keep objects created to fix serializers
	private static Set<Kryo> kryos = new HashSet<Kryo>();
	
	public static synchronized Kryo createKryo(){
		Kryo kryo = new Kryo();
		
		// we call reset ourselves after each readObject
		kryo.setAutoReset(false);
		// redirect to RSA bundle classloader
		kryo.setClassLoader(KryoFactory.class.getClassLoader());
		
		// Sometimes problems with serializing exceptions in Kryo (e.g. Throwable discrepance between android/jdk)
		kryo.addDefaultSerializer(Throwable.class, JavaSerializer.class);
		// required to instantiate classes without no-arg constructor
		kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
		// required to correctly handle unmodifiable collections (i.e. used in EndpointDescription)
		UnmodifiableCollectionsSerializer.registerSerializers( kryo );
		// required to correctly handle Arrays$ArrayList class (i.e. used in EndpointDescription)
		kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer() );		
		
		for(Iterator<Entry<String, Serializer>> it = serializers.entrySet().iterator();it.hasNext();){
			Entry<String, Serializer> entry = it.next();
			registerSerializer(kryo, entry.getKey());
		}
		
		kryos.add(kryo);
		
		return kryo;
	}
	
	public static void removeKryo(Kryo kryo){
		kryos.remove(kryo);
	}
	
	public static synchronized void addSerializer(String clazz, Object serializer){
		serializers.put(clazz, (Serializer)serializer);
		for(Kryo kryo : kryos){
			registerSerializer(kryo, clazz);
		}
	}
	
	public static synchronized void addSerializer(String clazz, Object serializer, int id){
		serializers.put(clazz, (Serializer)serializer);
		ids.put(clazz, id);
		for(Kryo kryo : kryos){
			registerSerializer(kryo, clazz);
		}
	}
	
	public static synchronized void removeSerializer(String clazz, Object serializer){
		serializers.remove(clazz);
		ids.remove(clazz);
	}
	
	private static void registerSerializer(Kryo kryo, String clazz){
		try {
			Serializer serializer = serializers.get(clazz);
			if(serializer==null){
				System.err.println("No Serializer available for class "+clazz);
				return;
			}
			Class c = serializer.getClass().getClassLoader().loadClass(clazz);
			Integer id = ids.get(clazz);
			if(id==null){
				kryo.register(c, serializer);
			} else {
				kryo.register(c, serializer, id);
			}
		} catch(ClassNotFoundException e){
			System.err.println("Error registering Kryo Serializer for class "+clazz);
			e.printStackTrace();
		}
	}
}
