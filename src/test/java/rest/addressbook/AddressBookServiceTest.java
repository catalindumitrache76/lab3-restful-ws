package rest.addressbook;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import rest.addressbook.config.ApplicationConfig;
import rest.addressbook.domain.AddressBook;
import rest.addressbook.domain.Person;

import static org.junit.Assert.assertEquals;

/**
 * A simple test suite
 */
public class AddressBookServiceTest {

	private HttpServer server;

	@Test
	public void serviceIsAlive() throws IOException {
		
		// Prepare server
		AddressBook ab = new AddressBook();
		launchServer(ab);

		// Request the address book
		Client client = ClientBuilder.newClient();
		Response response1 = client.target("http://localhost:8080/contacts").
				request().get();
		Response response2 = client.target("http://localhost:8080/contacts").
				request().get();
		Response response3 = client.target("http://localhost:8080/contacts").
				request().get();
		Response response4 = client.target("http://localhost:8080/contacts").
				request().get();
		boolean entityBuffer = response1.bufferEntity();
		AddressBook adEntity = response1.readEntity(AddressBook.class);
		List<Person> people = adEntity.getPersonList();
		
		// Check that it returns 200 and the data is correct.
		assertEquals(200, response1.getStatus());
		assertEquals(2, people.size());
		Iterator<Person> itPeople = people.iterator();
		Person p1 = itPeople.next();
		Person p2 = itPeople.next();
		assertEquals(1,p1.getId());
		assertEquals("Salvador",p1.getName());
		assertEquals(2,p2.getId());
		assertEquals("Juan",p2.getName());	
		
		System.out.println("Entity buffer:");
		System.out.println(entityBuffer);
		System.out.println("------------------------------------------------" + 
				"------------------");
		
		/**
		 * Checking idempotency for 3 get requests
		 */
		List<Person> people1 = people(response2);
		List<Person> people2 = people(response3);
		List<Person> people3 = people(response4);
		
		assertEquals(people1.size(),people2.size());
		assertEquals(people1.size(),people3.size());
		
		Iterator<Person> people1Iterator = people1.iterator();
		Iterator<Person> people2Iterator = people2.iterator();
		Iterator<Person> people3Iterator = people3.iterator();
		
		for (int i = 0; i < people1.size(); i++){
			Person person1 = people1Iterator.next();
			Person person2 = people2Iterator.next();
			Person person3 = people3Iterator.next();
			assertEquals(person1.getId(),person2.getId());
			assertEquals(person1.getName(),person2.getName());
			assertEquals(person1.getId(),person3.getId());
			assertEquals(person1.getName(),person3.getName());
		}
		
		/**
		 *  These parts are extras. I was just checking them out
		 */
		Configuration config = client.target("http://localhost:8080/contacts").
				getConfiguration();
		Configuration clientConfig = client.getConfiguration();
		
		System.out.println("A continuación imprimo las propiedades" + 
				" de localhost:8080/contacts:");
		Map<String, Object> configMap = config.getProperties();
		Iterator<String> configKeyIterator = configMap.keySet().iterator();
		String ckey = "";
		Object cvalue = null;
		while (configKeyIterator.hasNext()){
			ckey = configKeyIterator.next();
			cvalue = configMap.get(ckey);
			System.out.println(ckey + ": " + cvalue);
		}
		System.out.println("------------------------------------------------" + 
				"------------------");
		
		
		System.out.println("A continuación imprimo las propiedades del "
				+ "cliente:");
		Map<String, Object> configClientMap = clientConfig.getProperties();
		Iterator<String> configClientKeyIterator = configClientMap.keySet().
				iterator();
		String cckey = "";
		Object ccvalue = null;
		while (configClientKeyIterator.hasNext()){
			cckey = configClientKeyIterator.next();
			ccvalue = configClientMap.get(cckey);
			System.out.println(cckey + ": " + ccvalue);
		}
		System.out.println("------------------------------------------------" + 
				"------------------");
		
		
		System.out.println("A continuación imprimo los nombres de las "
				+ "propiedades de localhost:8080/contacts:");
		Collection<String> configNames = config.getPropertyNames();
		Iterator<String> configNameKeyIterator = configNames.iterator();
		String cNkey = "";
		while (configNameKeyIterator.hasNext()){
			cNkey = configNameKeyIterator.next();
			System.out.println(cNkey + ": " + cvalue);
		}
		System.out.println("------------------------------------------------" + 
				"------------------");
		
		
		System.out.println("A continuación tengo la cabecera:");
		MultivaluedMap<String, Object> headers = response1.getHeaders();
		Iterator<String> headerKeyIterator = headers.keySet().iterator();
		String hkey = "";
		Object hvalue = null;
		while (headerKeyIterator.hasNext()){
			hkey = headerKeyIterator.next();
			hvalue = headers.get(hkey);
			System.out.println(hkey + ": " + hvalue);
		}
		System.out.println("------------------------------------------------" + 
				"------------------");
		
		
		System.out.println("A continuación tengo la fecha de respuesta:");
		System.out.println(response1.getDate());
		System.out.println("------------------------------------------------" + 
				"------------------");
		
		
		System.out.println("A continuación tengo los métodos HTTP" + 
		" permitidos sobre el response:");
		Iterator<String> allowedMethodsKeyIterator = response1.
				getAllowedMethods().iterator();
		String aMkey = "";
		Object aMvalue = null;
		while (allowedMethodsKeyIterator.hasNext()){
			aMkey = allowedMethodsKeyIterator.next();
			aMvalue = headers.get(aMkey);
			System.out.println(aMkey + ": " + aMvalue);
		}
		System.out.println("------------------------------------------------" + 
				"------------------");
		
		
		System.out.println("A continuación tengo la location del response:");
		System.out.println(response1.getLocation());
		System.out.println("------------------------------------------------" + 
				"------------------");
		
		
		System.out.println("A continuación tengo el status info del response:");
		System.out.println(response1.getStatusInfo());
		System.out.println("------------------------------------------------" + 
				"------------------");
		
		
		//////////////////////////////////////////////////////////////////////
		// Test that GET /contacts is safe and idempotent
		//////////////////////////////////////////////////////////////////////	
	}
	
	
	private List<Person> people(Response response) {
		assertEquals(200, response.getStatus());
		AddressBook adEntity = response.readEntity(AddressBook.class);
		List<Person> people = adEntity.getPersonList();
		return people;
	}
	
	
	@Test
	public void createUser() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		launchServer(ab);

		// Prepare data
		Person juan = new Person();
		juan.setName("Juan");
		URI juanURI = URI.create("http://localhost:8282/contacts/person/1");

		// Create a new user
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));

		assertEquals(201, response.getStatus());
		assertEquals(juanURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Check that the new user exists
		response = client.target("http://localhost:8282/contacts/person/1")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		//////////////////////////////////////////////////////////////////////
		// Verify that POST /contacts is well implemented by the service, i.e
		// test that it is not safe and not idempotent
		//////////////////////////////////////////////////////////////////////	
				
	}

	@Test
	public void createUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(ab.nextId());
		ab.getPersonList().add(salvador);
		launchServer(ab);

		// Prepare data
		Person juan = new Person();
		juan.setName("Juan");
		URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
		Person maria = new Person();
		maria.setName("Maria");
		URI mariaURI = URI.create("http://localhost:8282/contacts/person/3");

		// Create a user
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(juan, MediaType.APPLICATION_JSON));
		assertEquals(201, response.getStatus());
		assertEquals(juanURI, response.getLocation());

		// Create a second user
		response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(201, response.getStatus());
		assertEquals(mariaURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person mariaUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaUpdated.getName());
		assertEquals(3, mariaUpdated.getId());
		assertEquals(mariaURI, mariaUpdated.getHref());

		// Check that the new user exists
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		mariaUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaUpdated.getName());
		assertEquals(3, mariaUpdated.getId());
		assertEquals(mariaURI, mariaUpdated.getHref());

		//////////////////////////////////////////////////////////////////////
		// Verify that GET /contacts/person/3 is well implemented by the service, i.e
		// test that it is safe and idempotent
		//////////////////////////////////////////////////////////////////////	
	
	}

	@Test
	public void listUsers() throws IOException {

		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		Person juan = new Person();
		juan.setName("Juan");
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Test list of contacts
		Client client = ClientBuilder.newClient();
		Response response = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		AddressBook addressBookRetrieved = response
				.readEntity(AddressBook.class);
		assertEquals(2, addressBookRetrieved.getPersonList().size());
		assertEquals(juan.getName(), addressBookRetrieved.getPersonList()
				.get(1).getName());

		//////////////////////////////////////////////////////////////////////
		// Verify that GET for collections is well implemented by the service, i.e
		// test that it is safe and idempotent
		//////////////////////////////////////////////////////////////////////	
	
	}

	@Test
	public void updateUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(ab.nextId());
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(ab.getNextId());
		URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Update Maria
		Person maria = new Person();
		maria.setName("Maria");
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(maria.getName(), juanUpdated.getName());
		assertEquals(2, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Verify that the update is real
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person mariaRetrieved = response.readEntity(Person.class);
		assertEquals(maria.getName(), mariaRetrieved.getName());
		assertEquals(2, mariaRetrieved.getId());
		assertEquals(juanURI, mariaRetrieved.getHref());

		// Verify that only can be updated existing values
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		assertEquals(400, response.getStatus());

		//////////////////////////////////////////////////////////////////////
		// Verify that PUT /contacts/person/2 is well implemented by the service, i.e
		// test that it is idempotent
		//////////////////////////////////////////////////////////////////////	
	
	}

	@Test
	public void deleteUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(1);
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(2);
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Delete a user
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/2").request()
				.delete();
		assertEquals(204, response.getStatus());

		// Verify that the user has been deleted
		response = client.target("http://localhost:8282/contacts/person/2")
				.request().delete();
		assertEquals(404, response.getStatus());

		//////////////////////////////////////////////////////////////////////
		// Verify that DELETE /contacts/person/2 is well implemented by the service, i.e
		// test that it is idempotent
		//////////////////////////////////////////////////////////////////////	

	}

	@Test
	public void findUsers() throws IOException {
		// Prepare server
		AddressBook ab = new AddressBook();
		Person salvador = new Person();
		salvador.setName("Salvador");
		salvador.setId(1);
		Person juan = new Person();
		juan.setName("Juan");
		juan.setId(2);
		ab.getPersonList().add(salvador);
		ab.getPersonList().add(juan);
		launchServer(ab);

		// Test user 1 exists
		Client client = ClientBuilder.newClient();
		Response response = client
				.target("http://localhost:8282/contacts/person/1")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person person = response.readEntity(Person.class);
		assertEquals(person.getName(), salvador.getName());
		assertEquals(person.getId(), salvador.getId());
		assertEquals(person.getHref(), salvador.getHref());

		// Test user 2 exists
		response = client.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		person = response.readEntity(Person.class);
		assertEquals(person.getName(), juan.getName());
		assertEquals(2, juan.getId());
		assertEquals(person.getHref(), juan.getHref());

		// Test user 3 exists
		response = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		assertEquals(404, response.getStatus());
	}

	private void launchServer(AddressBook ab) throws IOException {
		URI uri = UriBuilder.fromUri("http://localhost/").port(8080).build();
		server = GrizzlyHttpServerFactory.createHttpServer(uri,
				new ApplicationConfig(ab));
		server.start();
	}

	@After
	public void shutdown() {
		if (server != null) {
			server.shutdownNow();
		}
		server = null;
	}

}
