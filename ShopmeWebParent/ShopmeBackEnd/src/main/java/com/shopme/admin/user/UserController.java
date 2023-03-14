package com.shopme.admin.user;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shopme.admin.util.FileUploadUtil;
import com.shopme.admin.util.Util;
import com.shopme.common.entity.Role;
import com.shopme.common.entity.User;

@Controller
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private Util util;

	@Autowired
	private RoleRepository roleRepo;

	@GetMapping("/users")
	public String listAll(Model model) {
		List<Role> roles = (List<Role>) roleRepo.findAll();
		if (roles.isEmpty()) {
			util.createAllRoles();
		}
		return listByPage(1, model, "firstName", "asc");
	}

	@GetMapping("/users/page/{pageNum}")
	public String listByPage(@PathVariable("pageNum") int pageNum, Model model, @Param("sortField") String sortField, @Param("sortDir") String sortDir) {
		Page<User> page = userService.listByPage(pageNum,sortField,sortDir);
		List<User> listUsers = page.getContent();

		long startCount = (pageNum - 1) * UserService.USER_PER_PAGE + 1;
		long endCount = startCount + UserService.USER_PER_PAGE - 1;
		long totalItems = page.getTotalElements();
		if(endCount>totalItems && startCount>totalItems){
			totalItems = 0;
		}else if(endCount>totalItems) {
			endCount = page.getTotalElements();
		}

		String reverseSortDir = sortDir.equals("asc") ? "desc" : "asc";
		model.addAttribute("currentPage",pageNum);
		model.addAttribute("totalPages",page.getTotalPages());
		model.addAttribute("startCount",startCount);
		model.addAttribute("endCount",endCount);
		model.addAttribute("totalItems",totalItems);
		model.addAttribute("listUsers", listUsers);
		model.addAttribute("sortField", sortField);
		model.addAttribute("sortDir", sortDir);
		model.addAttribute("reverseSortDir", reverseSortDir);

		return "users";
	}

	@GetMapping("/users/new")
	public String newUser(Model model) {
		List<Role> listRoles = userService.listRoles();
		User user = new User();
		user.setEnabled(true);

		model.addAttribute("pageTitle", "Create New User");
		model.addAttribute("user", user);
		model.addAttribute("listRoles", listRoles);

		return "user_form";
	}

	@PostMapping("/users/save")
	public String saveUser(User user, RedirectAttributes redirectAttributes,
			@RequestParam("image") MultipartFile multipartFile) throws IOException {
		System.out.println(multipartFile.isEmpty());
		if (!multipartFile.isEmpty()) {
			String fileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
			user.setPhotos(fileName);
			User savedUser = userService.save(user);

			String uploadDir = "user-photos/" + savedUser.getId();

			FileUploadUtil.cleanDir(uploadDir);

			FileUploadUtil.saveFile(uploadDir, fileName, multipartFile);
		} else {
			System.out.println(user.getPhotos().isEmpty());
			if (user.getPhotos().isEmpty())
				user.setPhotos(null);
			userService.save(user);
		}

		redirectAttributes.addFlashAttribute("message", "The user has been saved successfully");

		return "redirect:/users";
	}

	@GetMapping("/users/edit/{id}")
	public String editUser(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
		try {
			List<Role> listRoles = userService.listRoles();
			User user = userService.get(id);
			model.addAttribute("pageTitle", "Edit User (ID: " + id + ")");
			model.addAttribute("user", user);
			model.addAttribute("listRoles", listRoles);
			return "user_form";
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", e.getMessage());
			return "redirect:/users";
		}

	}

	@GetMapping("/users/delete/{id}")
	public String deleteUser(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
		try {
			userService.delete(id);

			String imageDir = "user-photos/" + id;
			FileUploadUtil.deleteDir(imageDir);
			redirectAttributes.addFlashAttribute("message", "The user ID " + id + " has been deleted successfully");
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", e.getMessage());
		}
		return "redirect:/users";
	}

	@GetMapping("/users/{id}/enabled/{status}")
	public String updateUserEnabledStatus(@PathVariable("id") Integer id, @PathVariable("status") boolean enabled,
			RedirectAttributes redirectAttributes) {
		try {
			userService.updateUserEnableStatus(id, enabled);
			redirectAttributes.addFlashAttribute("message",
					"The user ID " + id + " has been " + (enabled ? "enabled" : "disabled") + " successfully");
		} catch (UserNotFoundException e) {
			redirectAttributes.addFlashAttribute("message", e.getMessage());
		}

		return "redirect:/users";
	}

}
