package nju.software.service.impl;

import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import nju.software.dao.impl.EmployeeDAO;
import nju.software.dataobject.Employee;
import nju.software.service.EmployeeService;
import nju.software.util.ActivitiAPIUtil;


@Service("employeeServiceImpl")
public class EmployeeServiceImpl implements EmployeeService {

	@Autowired
	private EmployeeDAO employeeDAO;
	@Autowired
	private EntityManagerFactory emf;
	@Autowired
	private ActivitiAPIUtil activitiAPIUtil;
	
	public final static String DEPARTMENT="市场部";
	
	@Override
	public Employee getEmployeeById(int employeeId) {
		try {
			Employee employee = employeeDAO.findById(employeeId);
			return employee;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int addEmployee(Employee employee) {
		try {
			employeeDAO.save(employee);
			activitiAPIUtil.addUser(String.valueOf(employee.getEmployeeId()));
			return employee.getEmployeeId();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public boolean deleteEmployee(int employeeId) {
		try {
			System.out.println("============employeeName:"+getEmployeeById(employeeId).getEmployeeName());
			employeeDAO.delete(getEmployeeById(employeeId));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean updateEmployee(Employee employee) {
		try {
			employeeDAO.attachDirty(employee);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public List<Employee> getAllEmployee() {
		try {
			List<Employee> list = employeeDAO.findAll();
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<Employee> getEmployeeByPage(int page, int numberPerPage) {
		try {
			List<Employee> list = employeeDAO.findByPage(page, numberPerPage);
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int getcount() {
		try {
			int count = employeeDAO.count();
			return count;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public List<Employee> getEmployeeByName(String employeename) {
		List<Employee> employees = employeeDAO.findByEmployeeName(employeename);
		return employees;
	}

	@Override
	public List<Employee> getAllManagerStaff() {
		Employee instance=new Employee();
		instance.setDepartment(EmployeeServiceImpl.DEPARTMENT);
		List<Employee> eList=employeeDAO.findByExample(instance);
		
		return eList;
	}

}
