package com.ledgerline.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "account_permissions")
public class AccountPermission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type", nullable = false)
    private PermissionType permissionType;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public Account getAccount() { return account; }
    public void setAccount(Account a) { this.account = a; }
    public PermissionType getPermissionType() { return permissionType; }
    public void setPermissionType(PermissionType p) { this.permissionType = p; }
}
