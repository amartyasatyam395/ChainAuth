package com.chainauth.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {
    private final AdminAccountService accounts;
    public AdminAccountController(AdminAccountService accounts){this.accounts=accounts;}
    @GetMapping public List<Map<String,Object>> list(){return accounts.listUsers();}
    @PutMapping("/{userId}/roles") public Map<String,Object> updateRoles(@PathVariable String userId,@Valid @RequestBody AdminRoleUpdateRequest request){return accounts.updateRoles(userId,request.roles());}
    @PostMapping("/{userId}/suspend") public Map<String,Object> suspend(@PathVariable String userId){return accounts.suspend(userId);}
    @PostMapping("/{userId}/reactivate") public Map<String,Object> reactivate(@PathVariable String userId){return accounts.reactivate(userId);}
}
