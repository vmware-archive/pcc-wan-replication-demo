package io.pivotal.controller;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.annotation.Resource;

import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.codearte.jfairy.Fairy;
import io.codearte.jfairy.producer.person.Person;
import io.pivotal.domain.Customer;
import io.pivotal.repo.pcc.CustomerRepository;

@RestController
public class CustomerController {
	
	@Autowired
	CustomerRepository pccCustomerRepository;
	
	@Resource(name = "customer")
	Region<String, Customer> customerRegion;
	
	boolean CONTINUE_LOAD = false;
	int STARTING_INDEX = 0;
	
	Fairy fairy = Fairy.create();
	
	@RequestMapping(method = RequestMethod.GET, path = "/clearcache")
	@ResponseBody
	public String clearCache() throws Exception {
		customerRegion.removeAll(customerRegion.keySetOnServer());
		return "Region cleared";
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/countcache")
	@ResponseBody
	public Long countCache() throws Exception {
		return pccCustomerRepository.count();
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/startload")
	@ResponseBody
	public String startLoad(@RequestParam(value = "region", required = true) String region, @RequestParam(value = "starting_index", required = true) String index, @RequestParam(value = "amount", required = true) String amount) throws Exception {
		
		CONTINUE_LOAD = true;
		STARTING_INDEX = Integer.parseInt(index);

		LoadWorker worker = new LoadWorker(region, Integer.parseInt(amount));
		worker.start();
		
		return "New customers successfully saved into Cloud Cache";
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/stopload")
	@ResponseBody
	public String stopLoad() throws Exception {
		
		CONTINUE_LOAD = false;
		
		return "Loading process stopped";
		
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/loadstatus")
	@ResponseBody
	public String getStatus() throws Exception {
		
		return CONTINUE_LOAD ? "running" : "stopped";
		
	}
	
	@RequestMapping(method = RequestMethod.GET, path = "/listall")
	@ResponseBody
	public String getAll() throws Exception {
		Queue<Customer> customers = new PriorityQueue<Customer>(customerComparator);
		pccCustomerRepository.findAll().forEach(e -> customers.add(e));
		
		StringBuilder result = new StringBuilder();
		
		while (!customers.isEmpty()) {
			result.append(customers.poll()+"</br>");
		}
		
		return result.toString();
	}
	
	public static Comparator<Customer> customerComparator = new Comparator<Customer>() {

		@Override
		public int compare(Customer c1, Customer c2) {
			if (c1.getTimestamp().before(c2.getTimestamp())) {
	            return -1;
	        } else if (c1.getTimestamp().after(c2.getTimestamp())) {
	            return 1;
	        } else {
	            return c1.getIndex() - c2.getIndex();
	        }   
		}
	};
	
	class LoadWorker extends Thread {
		
		String REGION_NAME;
		int BATCH_NUM;
		
		LoadWorker(String region, int num) {
			this.REGION_NAME = region;
			this.BATCH_NUM = num;
		}

	    @Override
	    public void run() {
	        
	        while (CONTINUE_LOAD) {
	        	List<Customer> customers = new ArrayList<>();
				
				for (int i=0; i<BATCH_NUM; i++) {
					Person person = fairy.person();
					Date now = Date.from(ZonedDateTime.now(ZoneOffset.UTC).toInstant());
					Customer customer = new Customer(person.fullName(), person.email(), person.getAddress().toString(), person.dateOfBirth().toString(), REGION_NAME, STARTING_INDEX++, now);
					customers.add(customer);
				}
				
				pccCustomerRepository.saveAll(customers);
				
	            try {
	                Thread.sleep(5000);
	            } catch (InterruptedException e) {
	                break;
	            }
	        }
	    }

	}

	
}
