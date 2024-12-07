package com.boostmytool.controllers;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.boostmytool.model.Supplier;
import com.boostmytool.model.SupplierDto;
import com.boostmytool.services.SuppliersRepository;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/suppliers")
public class SuppliersController {
	

    @Autowired
    private SuppliersRepository repo;

    @GetMapping({"", "/"})
    public String showSupplierList(Model model) {
        List<Supplier> suppliers = repo.findAll();
        model.addAttribute("suppliers", suppliers);
        return "suppliers/index";
    }

    // Tìm kiếm nhà cung cấp
    @GetMapping("/search")
    public String searchSuppliers(@RequestParam("keyword") String keyword, Model model) {
        List<Supplier> suppliers;
        if (keyword == null || keyword.isEmpty()) {
            // Nếu không có từ khóa, trả về tất cả nhà cung cấp
            suppliers = repo.findAll();
        } else {
            // Tìm kiếm nhà cung cấp theo từ khóa trên nhiều trường
            suppliers = repo.searchSuppliersByKeyword(keyword);
        }
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("keyword", keyword); // Giữ lại từ khóa để hiển thị trên form
        return "suppliers/index";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        SupplierDto supplierDto = new SupplierDto();
        model.addAttribute("supplierDto", supplierDto);
        return "suppliers/CreateSupplier";
    }

    @PostMapping("/create")
    public String createSupplier(
            @Valid @ModelAttribute SupplierDto supplierDto,
            BindingResult result) {
        if (supplierDto.getImageLogo().isEmpty()) {
            result.addError(new FieldError("supplierDto", "imageLogo", "The Logo file is required"));
        }

        if (repo.existsById(supplierDto.getId())) {
            result.addError(new FieldError("supplierDto", "id", "This ID already exists. Please choose another ID."));
        }

        if (result.hasErrors()) {
            return "suppliers/CreateSupplier";
        }

        Supplier supplier = new Supplier();
        supplier.setId(supplierDto.getId());
        supplier.setName(supplierDto.getName());
        supplier.setAddress(supplierDto.getAddress());
        supplier.setDescription(supplierDto.getDescription());
        supplier.setPhone(supplierDto.getPhone());
        supplier.setEmail(supplierDto.getEmail());

        Date createAt = new Date();
        supplier.setCreatedAt(createAt);
        supplier.setUpdatedAt(createAt);

        // Lưu ảnh và thêm thông tin ảnh
        MultipartFile image = supplierDto.getImageLogo();
        String storageFileName = createAt.getTime() + "_" + image.getOriginalFilename();
        String uploadDir = "public/imageLogo/";
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
            supplier.setImageLogo(storageFileName);
        } catch (Exception e) {
            System.out.println("Error uploading image: " + e.getMessage());
        }

        repo.save(supplier);
        return "redirect:/suppliers";
    }


    // Edit Supplier
    @GetMapping("/edit")
    public String showEditPage(
            Model model,
            @RequestParam(value = "id", required = true) String id) {

        try {
            Supplier supplier = repo.findById(id).orElseThrow(() -> new RuntimeException("Supplier not found"));
            model.addAttribute("supplier", supplier);

            SupplierDto supplierDto = new SupplierDto();
            supplierDto.setName(supplier.getName());
            supplierDto.setAddress(supplier.getAddress());
            supplierDto.setDescription(supplier.getDescription());
            model.addAttribute("supplierDto", supplierDto);

        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            return "redirect:/suppliers";
        }

        return "suppliers/EditSupplier";
    }

    @PostMapping("/edit")
    public String updateSupplier(
            Model model,
            @RequestParam(value = "id", required = true) String id,
            @Valid @ModelAttribute SupplierDto supplierDto,
            BindingResult result) {

        try {
            Supplier supplier = repo.findById(id).orElseThrow(() -> new RuntimeException("Supplier not found"));

            if (result.hasErrors()) {
                model.addAttribute("supplier", supplier);
                return "suppliers/EditSupplier";
            }

            supplier.setName(supplierDto.getName());
            supplier.setAddress(supplierDto.getAddress());
            supplier.setDescription(supplierDto.getDescription());
            supplier.setPhone(supplierDto.getPhone());
            supplier.setEmail(supplierDto.getEmail());

            // Cập nhật ảnh nếu được tải lên
            if (!supplierDto.getImageLogo().isEmpty()) {
                String uploadDir = "public/imageLogo/";
                Path oldImagePath = Paths.get(uploadDir + supplier.getImageLogo());
                try {
                    Files.deleteIfExists(oldImagePath);
                } catch (Exception ex) {
                    System.out.println("Error deleting old image: " + ex.getMessage());
                }

                MultipartFile image = supplierDto.getImageLogo();
                String storageFileName = new Date().getTime() + "_" + image.getOriginalFilename();
                try (InputStream inputStream = image.getInputStream()) {
                    Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
                }
                supplier.setImageLogo(storageFileName);
            }

            supplier.setUpdatedAt(new Date());
            repo.save(supplier);

        } catch (Exception e) {
            System.out.println("Error updating supplier: " + e.getMessage());
        }

        return "redirect:/suppliers";
    }


    @GetMapping("/delete")
    public String deleteSupplier(
            @RequestParam(value = "id", required = true) String id) {
        try {
            Supplier supplier = repo.findById(id).orElseThrow(() -> new RuntimeException("Supplier not found"));
            // delete supplier images
            Path imagePath = Paths.get("public/imageLogo/" + supplier.getImageLogo());
            try {
                Files.deleteIfExists(imagePath);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }

            repo.delete(supplier);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return "redirect:/suppliers";
    }
}
