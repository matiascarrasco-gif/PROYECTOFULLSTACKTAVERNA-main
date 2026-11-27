package cl.duoc.ep3.service;

import cl.duoc.ep3.dto.AccountResponse;
import cl.duoc.ep3.dto.PurchaseItemRequest;
import cl.duoc.ep3.dto.PurchaseRequest;
import cl.duoc.ep3.dto.PurchaseResponse;
import cl.duoc.ep3.dto.RedemptionRequest;
import cl.duoc.ep3.dto.RedemptionResponse;
import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.model.Producto;
import cl.duoc.ep3.model.PurchaseOrder;
import cl.duoc.ep3.model.RewardRedemption;
import cl.duoc.ep3.repository.AccountRepository;
import cl.duoc.ep3.repository.ProductoRepository;
import cl.duoc.ep3.repository.PurchaseOrderRepository;
import cl.duoc.ep3.repository.RewardRedemptionRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RewardsService {

    private final AccountRepository accountRepository;
    private final ProductoRepository productoRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final RewardRedemptionRepository redemptionRepository;

    public RewardsService(
            AccountRepository accountRepository,
            ProductoRepository productoRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            RewardRedemptionRepository redemptionRepository
    ) {
        this.accountRepository = accountRepository;
        this.productoRepository = productoRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @Transactional
    public PurchaseResponse purchase(Account authenticatedAccount, PurchaseRequest request) {
        Account account = lockAccount(authenticatedAccount.getId());
        // Reintentar el mismo orderId devuelve la compra anterior y no vuelve a sumar puntos.
        var existingOrder = purchaseOrderRepository.findById(request.orderId());
        if (existingOrder.isPresent()) {
            PurchaseOrder order = existingOrder.get();
            if (!order.getAccount().getId().equals(account.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El identificador de compra ya fue utilizado.");
            }
            return purchaseResponse(order, account);
        }

        // Los canjes ya tienen precio cero; solo los productos normales entran al subtotal.
        long subtotal = 0;
        for (PurchaseItemRequest item : request.items()) {
            Producto product = findProduct(item.productId());
            if (item.redemptionId() == null) {
                subtotal = addPrice(subtotal, product.getPrice(), item.quantity());
            } else {
                validateAndUseRedemption(item, product, account);
            }
        }

        long discount = account.isDuoc() ? Math.round(subtotal * 0.20d) : 0;
        long total = subtotal - discount;
        int pointsEarned;
        try {
            // Los puntos se calculan sobre el total final, después del descuento.
            pointsEarned = RewardPolicy.pointsEarned(total);
            account.addPoints(pointsEarned);
        } catch (ArithmeticException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto de la compra supera el límite permitido.");
        }

        PurchaseOrder order = purchaseOrderRepository.save(new PurchaseOrder(
                request.orderId(), account, subtotal, discount, total, pointsEarned));
        accountRepository.save(account);
        return purchaseResponse(order, account);
    }

    @Transactional
    public RedemptionResponse redeem(Account authenticatedAccount, RedemptionRequest request) {
        Account account = lockAccount(authenticatedAccount.getId());
        Producto product = findProduct(request.productId());
        Integer pointsCost = product.getPuntosCanje();
        if (pointsCost == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este producto no se puede canjear con puntos.");
        }
        if (!account.redeemPoints(pointsCost)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No tienes puntos suficientes para este canje.");
        }

        RewardRedemption redemption = redemptionRepository.save(
                new RewardRedemption(account, product, pointsCost));
        accountRepository.save(account);
        return new RedemptionResponse(
                redemption.getId(),
                product.getId(),
                pointsCost,
                account.getPuntos(),
                AccountResponse.from(account));
    }

    private Account lockAccount(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no está disponible."));
    }

    private Producto findProduct(Long productId) {
        return productoRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado."));
    }

    private void validateAndUseRedemption(PurchaseItemRequest item, Producto product, Account account) {
        if (item.quantity() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada canje debe corresponder a una unidad.");
        }
        RewardRedemption redemption = redemptionRepository
                .findByIdAndAccountIdForUpdate(item.redemptionId(), account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "El canje no pertenece a esta cuenta."));
        if (redemption.isUsed() || !redemption.getProduct().getId().equals(product.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El canje ya fue utilizado o no coincide con el producto.");
        }
        redemption.markUsed();
    }

    private long addPrice(long subtotal, long price, int quantity) {
        try {
            return Math.addExact(subtotal, Math.multiplyExact(price, quantity));
        } catch (ArithmeticException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto de la compra supera el límite permitido.");
        }
    }

    private PurchaseResponse purchaseResponse(PurchaseOrder order, Account account) {
        return new PurchaseResponse(
                order.getId(),
                order.getSubtotal(),
                order.getDiscount(),
                order.getTotal(),
                order.getPointsEarned(),
                account.getPuntos(),
                AccountResponse.from(account));
    }
}
