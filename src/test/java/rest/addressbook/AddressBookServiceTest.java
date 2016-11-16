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
import static org.junit.Assert.assertNotEquals;

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
		
		Response response1 = client.target("http://localhost:8282/contacts").
				request().get();
		Response response2 = client.target("http://localhost:8282/contacts").
				request().get();
		Response response3 = client.target("http://localhost:8282/contacts").
				request().get();
		Response response4 = client.target("http://localhost:8282/contacts").
				request().get();
		
		/**
		 * Checking idempotency for 4 get requests
		 */
		idempotencyServiceIsAlive(response1);
		idempotencyServiceIsAlive(response2);
		idempotencyServiceIsAlive(response3);
		idempotencyServiceIsAlive(response4);
		
		
		/**
		 * Checking security for 4 get requests 
		 */
		assertEquals(response1.getLastModified(),response2.getLastModified());
		assertEquals(response1.getLastModified(),response3.getLastModified());
		assertEquals(response1.getLastModified(),response4.getLastModified());
		
	}
	
	private void idempotencyServiceIsAlive(Response response) {
		
		// Check that it returns 200 and the data is correct.
		assertEquals(200, response.getStatus());
		AddressBook adEntity = response.readEntity(AddressBook.class);
		List<Person> people = adEntity.getPersonList();
		assertEquals(0, people.size());
		assertEquals(1,adEntity.getNextId());
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

		// Check the list before
		Response responseBefore = client.
				target("http://localhost:8282/contacts/").
				request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, responseBefore.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, responseBefore.
				getMediaType());
		AddressBook before = responseBefore.readEntity(AddressBook.class);
		int sizeBefore = before.getPersonList().size();

		//POST:
		Response response = client.target("http://localhost:8282/contacts").
				request(MediaType.APPLICATION_JSON).
				post(Entity.entity(juan, MediaType.APPLICATION_JSON));
		
		// Check the list before
		Response responseAfter = client.
				target("http://localhost:8282/contacts/").
				request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, responseAfter.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, responseAfter.
				getMediaType());
		AddressBook after = responseAfter.readEntity(AddressBook.class);
		int sizeAfter = after.getPersonList().size();
		
		// Proves that POST is not secure by showing that a resource has been 
		// modified (the getPersonList list)
		assertNotEquals(sizeBefore,sizeAfter);
		
		assertEquals(201, response.getStatus());
		assertEquals(juanURI, response.getLocation());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		Person juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		// Check that the new user exists
		response = client.target("http://localhost:8282/contacts/person/1").
				request(MediaType.APPLICATION_JSON).get();
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		juanUpdated = response.readEntity(Person.class);
		assertEquals(juan.getName(), juanUpdated.getName());
		assertEquals(1, juanUpdated.getId());
		assertEquals(juanURI, juanUpdated.getHref());

		/**
		 *  Prove that idempotency is not satisfied in post method
		 */
		Response response2 = client.target("http://localhost:8282/contacts").
				request(MediaType.APPLICATION_JSON).
				post(Entity.entity(juan, MediaType.APPLICATION_JSON));
		// Resource created, but with another location
		assertEquals (201,response2.getStatus());
		assertNotEquals(juanURI,response2.getLocation());
		
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
		
		Response response2 = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		Response response3 = client.target("http://localhost:8282/contacts/person/3")
				.request(MediaType.APPLICATION_JSON).get();
		
		/**
		 * Checking idempotency  
		 */
		idempotencyCreateUsers(response2, maria, mariaURI);
		idempotencyCreateUsers(response3, maria, mariaURI);
		
		/**
		 * Checking security for 3 get requests 
		 */
		assertEquals(response.getLastModified(),response2.getLastModified());
		assertEquals(response.getLastModified(),response3.getLastModified());
	}
	
	private void idempotencyCreateUsers(Response response, Person maria, URI mariaURI) {
		
		// Check that it returns 200 and the data is correct.
		assertEquals(200, response.getStatus());
		Person person = response.readEntity(Person.class);
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
		assertEquals(maria.getName(), person.getName());
		assertEquals(3, person.getId());
		assertEquals(mariaURI, person.getHref());
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
		AddressBook addressBookRetrieved = response.readEntity(AddressBook.class);
		assertEquals(2, addressBookRetrieved.getPersonList().size());
		assertEquals(juan.getName(), addressBookRetrieved.getPersonList()
				.get(1).getName());

		//////////////////////////////////////////////////////////////////////
		// Verify that GET for collections is well implemented by the service, i.e
		// test that it is safe and idempotent
		//////////////////////////////////////////////////////////////////////	
	
		Response response2 = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON).get();
		Response response3 = client.target("http://localhost:8282/contacts")
				.request(MediaType.APPLICATION_JSON).get();
		
		/*
		 * Checking the idempotency (similar to service is alive)
		 */
		idempotencyListUsers(response2);
		idempotencyListUsers(response3);
		
		/**
		 * Checking security for 3 get requests 
		 */
		assertEquals(response.getLastModified(),response2.getLastModified());
		assertEquals(response.getLastModified(),response3.getLastModified());
	}
	
	private void idempotencyListUsers(Response response) {
		
		// Check that it returns 200 and the data is correct.
		assertEquals(200, response.getStatus());
		AddressBook adEntity = response.readEntity(AddressBook.class);
		List<Person> people = adEntity.getPersonList();
		assertEquals(2, people.size());
		assertEquals("Salvador", people.get(0).getName());
		assertEquals("Juan", people.get(1).getName());
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
	
		
		/**
		 *  Prove that idempotency is satisfied in post method
		 */
		Response response2 = client
				.target("http://localhost:8282/contacts/person/2")
				.request(MediaType.APPLICATION_JSON)
				.put(Entity.entity(maria, MediaType.APPLICATION_JSON));
		// Resource created, but with another location
		assertEquals(200, response2.getStatus());
		assertEquals(MediaType.APPLICATION_JSON_TYPE, response2.getMediaType());
		Person p2 = response2.readEntity(Person.class);
		assertEquals(maria.getName(), p2.getName());
		assertEquals(2, p2.getId());
		assertEquals(juanURI, p2.getHref());
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
		
		response = client.target("http://localhost:8282/contacts/person/2")
				.request().delete();
		assertEquals(404, response.getStatus());
		
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
		URI uri = UriBuilder.fromUri("http://localhost/").port(8282).build();
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
	
	
	/**
	 *  Extra method. I was just checking all these options out
	 */
	private void pruebas (Client client, Response response1){
		
		Configuration config = client.target("http://localhost:8282/contacts").
				getConfiguration();
		Configuration clientConfig = client.getConfiguration();
		
		System.out.println("A continuación imprimo las propiedades" + 
				" de localhost:8282/contacts:");
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
				+ "propiedades de localhost:8282/contacts:");
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
		
	}
}
