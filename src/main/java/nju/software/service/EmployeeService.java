package nju.software.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import nju.software.dataobject.Employee;

public interface EmployeeService {
//
//	/**
//	 *  获取所有专员的列表
//	 * @return 专员列表
//	 */
//	@Transactional(rollbackFor = Exception.class)
//	public List<Employee>   getAllManagerStaff();  
	
	@Transactional(rollbackFor = Exception.class)
	public Employee getEmployeeById(int employeeId);

	@Transactional(rollbackFor = Exception.class)
	public int addEmployee(Employee employee);

	@Transactional(rollbackFor = Exception.class)
	public boolean deleteEmployee(int employeeId);

	@Transactional(rollbackFor = Exception.class)
	public boolean updateEmployee(Employee employee);

	@Transactional(rollbackFor = Exception.class)
	public List<Employee> getAllEmployee();

	@Transactional(rollbackFor = Exception.class)
	public List<Employee> getEmployeeByPage(int page, int numberPerPage);

	@Transactional(rollbackFor = Exception.class)
	public int getcount();

	@Transactional(rollbackFor = Exception.class)
	public List<Employee> getEmployeeByName(String employeename);

	public List<Employee> getAllManagerStaff();

}
