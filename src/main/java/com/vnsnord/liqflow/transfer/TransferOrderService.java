package com.vnsnord.liqflow.transfer;

import com.vnsnord.liqflow.common.exception.ConflictException;
import com.vnsnord.liqflow.common.exception.LocationNotFoundException;
import com.vnsnord.liqflow.common.exception.ProductNotFoundException;
import com.vnsnord.liqflow.common.exception.TransferOrderNotFoundException;
import com.vnsnord.liqflow.inventory.Inventory;
import com.vnsnord.liqflow.inventory.InventoryRepository;
import com.vnsnord.liqflow.location.Location;
import com.vnsnord.liqflow.location.LocationRepository;
import com.vnsnord.liqflow.product.Product;
import com.vnsnord.liqflow.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Application service for the full transfer order lifecycle, including item
 * management, status transitions, and the physical stock movement executed on
 * completion.
 *
 * <p>Submitting an order reserves the source stock for that order, so
 * completing it can only ever consume units that are held for it, and cancelling
 * it hands those units back. See {@link TransferOrderReservation} for the
 * reservation lifecycle.</p>
 */
@Service
@Transactional(readOnly = true)
public class TransferOrderService
{

    private final TransferOrderRepository transferOrderRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final TransferOrderReservationRepository reservationRepository;
    private final TransferOrderMapper transferOrderMapper;

    /**
     * Creates a new transfer order service with the given dependencies.
     *
     * @param transferOrderRepository the transfer order repository
     * @param locationRepository      the location repository
     * @param productRepository       the product repository
     * @param inventoryRepository     the inventory repository
     * @param reservationRepository   the transfer order reservation repository
     * @param transferOrderMapper     the transfer order mapper
     */
    public TransferOrderService(TransferOrderRepository transferOrderRepository,
                                LocationRepository locationRepository,
                                ProductRepository productRepository,
                                InventoryRepository inventoryRepository,
                                TransferOrderReservationRepository reservationRepository,
                                TransferOrderMapper transferOrderMapper)
    {
        this.transferOrderRepository = transferOrderRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.transferOrderMapper = transferOrderMapper;
    }

    /**
     * Finds a transfer order by its identifier.
     *
     * @param id the order identifier
     * @return the corresponding summary response DTO
     * @throws TransferOrderNotFoundException if no order exists with the given id
     */
    public TransferOrderResponse getTransferOrderById(UUID id)
    {
        return transferOrderRepository.findById(id)
                .map(transferOrderMapper::toResponse)
                .orElseThrow(() -> new TransferOrderNotFoundException("id", id));
    }

    /**
     * Finds a transfer order together with its line items.
     *
     * @param id the order identifier
     * @return the corresponding detailed response DTO
     * @throws TransferOrderNotFoundException if no order exists with the given id
     */
    public TransferOrderDetailResponse getTransferOrderDetailById(UUID id)
    {
        return transferOrderMapper.toDetailResponse(findWithDetails(id));
    }

    /**
     * Returns a page of all transfer orders.
     *
     * @param pageable pagination and sorting information
     * @return a page of summary response DTOs
     */
    public Page<TransferOrderResponse> getAllTransferOrders(Pageable pageable)
    {
        return transferOrderRepository.findAllWithLocations(pageable)
                .map(transferOrderMapper::toResponse);
    }

    /**
     * Creates a new transfer order in {@code DRAFT} status.
     *
     * @param request the creation request
     * @return the response DTO of the saved order
     * @throws ConflictException         if an order with the same number already exists
     * @throws LocationNotFoundException if either location does not exist
     */
    @Transactional
    public TransferOrderResponse createTransferOrder(CreateTransferOrderRequest request)
    {
        String orderNumber = request.orderNumber().trim();
        if (transferOrderRepository.existsByOrderNumber(orderNumber))
        {
            throw new ConflictException("Transfer order with number '" + orderNumber + "' already exists");
        }
        Location source = locationRepository.findById(request.sourceLocationId())
                .orElseThrow(() -> new LocationNotFoundException("id", request.sourceLocationId()));
        Location target = locationRepository.findById(request.targetLocationId())
                .orElseThrow(() -> new LocationNotFoundException("id", request.targetLocationId()));

        TransferOrder order = new TransferOrder(orderNumber, source, target);
        TransferOrder savedOrder = transferOrderRepository.save(order);
        return transferOrderMapper.toResponse(savedOrder);
    }

    /**
     * Adds a product line to a draft transfer order.
     *
     * @param orderId the order identifier
     * @param request the item request
     * @return the updated detailed response DTO
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws ProductNotFoundException       if the product does not exist
     */
    @Transactional
    public TransferOrderDetailResponse addItem(UUID orderId, TransferOrderItemRequest request)
    {
        TransferOrder order = findWithDetails(orderId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException("id", request.productId()));
        order.addItem(product, request.quantity());
        return transferOrderMapper.toDetailResponse(transferOrderRepository.save(order));
    }

    /**
     * Removes a line item from a draft transfer order.
     *
     * @param orderId the order identifier
     * @param itemId  the line item identifier
     * @return the updated detailed response DTO
     * @throws TransferOrderNotFoundException if the order or the item does not exist
     */
    @Transactional
    public TransferOrderDetailResponse removeItem(UUID orderId, UUID itemId)
    {
        TransferOrder order = findWithDetails(orderId);
        TransferOrderItem item = order.getItems().stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new TransferOrderNotFoundException("item id", itemId));
        order.removeItem(item);
        return transferOrderMapper.toDetailResponse(transferOrderRepository.save(order));
    }

    /**
     * Submits a draft transfer order, transitioning it to {@code SUBMITTED} and
     * reserving the required stock at the source location for this order.
     *
     * <p>Reserving here is what makes the later stock movement safe: the units
     * this order needs stop being available to any other order, so completing it
     * can never be blocked by, or take from, somebody else's reservation.</p>
     *
     * @param id the order identifier
     * @return the response DTO of the updated order
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws IllegalStateException          if the order is not in draft status, has no items,
     *                                        or the source location lacks stock for an item
     * @throws IllegalArgumentException       if the source has no inventory record for an item's product
     */
    @Transactional
    public TransferOrderResponse submit(UUID id)
    {
        TransferOrder order = findWithDetails(id);
        order.submit();
        reserveStock(order);
        return transferOrderMapper.toResponse(transferOrderRepository.save(order));
    }

    /**
     * Marks a submitted transfer order as in transit.
     *
     * @param id the order identifier
     * @return the response DTO of the updated order
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws IllegalStateException          if the order is not in submitted status
     */
    @Transactional
    public TransferOrderResponse markInTransit(UUID id)
    {
        TransferOrder order = findWithDetails(id);
        order.markInTransit();
        return transferOrderMapper.toResponse(transferOrderRepository.save(order));
    }

    /**
     * Completes an in-transit transfer order, moving each reserved quantity
     * from the source inventory to the target inventory and then transitioning
     * the order to {@code COMPLETED}.
     *
     * <p>Only the quantity this order reserved is consumed, so stock held for
     * other orders is never touched.</p>
     *
     * @param id the order identifier
     * @return the response DTO of the updated order
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws IllegalStateException          if the order is not in transit, if a line item has no
     *                                        reservation, or if either side lacks an inventory record
     */
    @Transactional
    public TransferOrderResponse complete(UUID id)
    {
        TransferOrder order = findWithDetails(id);
        List<TransferOrderReservation> reservations = reservationRepository.findByTransferOrderIdForUpdate(order.getId());
        moveStock(order, reservations);
        order.complete();
        return transferOrderMapper.toResponse(transferOrderRepository.save(order));
    }

    /**
     * Cancels a transfer order, releasing any stock still held for it so the
     * units become available again.
     *
     * @param id the order identifier
     * @return the response DTO of the updated order
     * @throws TransferOrderNotFoundException if the order does not exist
     * @throws IllegalStateException          if the order is already completed
     */
    @Transactional
    public TransferOrderResponse cancel(UUID id)
    {
        TransferOrder order = findWithDetails(id);
        order.cancel();
        releaseReservations(order);
        return transferOrderMapper.toResponse(transferOrderRepository.save(order));
    }

    /**
     * Reserves the source stock for every line item of a submitted order,
     * locking the source inventory records in a deterministic order.
     *
     * @param order the transfer order being submitted
     * @throws IllegalStateException if the source location has no inventory record for an item's
     *                               product, or has insufficient available stock
     */
    private void reserveStock(TransferOrder order)
    {
        Map<InventoryRef, Inventory> locked = lockInventories(sourceRefs(order));

        for (TransferOrderItem item : order.getItems())
        {
            Product product = item.getProduct();
            int quantity = item.getQuantity();
            locked.get(sourceRef(item, order.getSourceLocation())).reserveStock(quantity);
            reservationRepository.save(
                    new TransferOrderReservation(order, item, product, order.getSourceLocation(), quantity));
        }
    }

    /**
     * Returns any stock still reserved for a cancelled order to the available
     * quantity of its source inventories.
     *
     * @param order the transfer order being cancelled
     * @throws IllegalStateException if a source inventory record no longer exists
     */
    private void releaseReservations(TransferOrder order)
    {
        List<TransferOrderReservation> held = reservationRepository
                .findByTransferOrderIdAndStatus(order.getId(), ReservationStatus.HELD);
        if (held.isEmpty())
        {
            return;
        }

        Set<InventoryRef> refs = new LinkedHashSet<>();
        for (TransferOrderReservation reservation : held)
        {
            refs.add(refOf(reservation.getProduct(), reservation.getLocation(), "source"));
        }
        Map<InventoryRef, Inventory> locked = lockInventories(refs);

        for (TransferOrderReservation reservation : held)
        {
            InventoryRef ref = refOf(reservation.getProduct(), reservation.getLocation(), "source");
            locked.get(ref).releaseReservedStock(reservation.getQuantity());
            reservation.markReleased();
        }
    }

    /**
     * Physically moves the reserved quantities from the source to the target
     * inventory records.
     *
     * <p>Both sides are locked up front and in a fixed order, so two
     * concurrent transfers touching the same products can never deadlock, and
     * the movement draws exclusively on this order's own reservations.</p>
     *
     * @param order        the transfer order being completed
     * @param reservations the order's locked reservations
     * @throws IllegalStateException if a line item has no reservation, or either side has no
     *                               inventory record for an item's product
     */
    private void moveStock(TransferOrder order, List<TransferOrderReservation> reservations)
    {
        Map<UUID, TransferOrderReservation> reservationsByItem = new LinkedHashMap<>();
        for (TransferOrderReservation reservation : reservations)
        {
            reservationsByItem.put(reservation.getItem().getId(), reservation);
        }

        Map<InventoryRef, Inventory> locked = lockInventories(moveRefs(order));

        for (TransferOrderItem item : order.getItems())
        {
            TransferOrderReservation reservation = reservationsByItem.get(item.getId());
            if (reservation == null)
            {
                throw new IllegalStateException(String.format(
                        "Transfer order has no reservation for product '%s'", item.getProduct().getSku()));
            }

            int quantity = reservation.getQuantity();
            locked.get(sourceRef(item, order.getSourceLocation())).consumeReservedStock(quantity);
            locked.get(targetRef(item, order.getTargetLocation())).addStock(quantity);
            reservation.markConsumed();
        }
    }

    /**
     * Acquires a pessimistic write lock on every referenced inventory record,
     * always in ascending location-then-product order.
     *
     * <p>The ordering is the whole point: it gives every transaction that moves
     * stock the same lock-acquisition sequence, which is what rules out the
     * classic AB-BA deadlock between two transfers that share products but list
     * them in opposite order. Any contention surfaces as a lock-acquisition or
     * deadlock failure instead, which the exception layer reports as a retryable
     * conflict.</p>
     *
     * @param refs the inventory records to lock
     * @return the locked records, keyed by the reference they were locked for
     * @throws IllegalStateException if a referenced inventory record does not exist
     */
    private Map<InventoryRef, Inventory> lockInventories(Set<InventoryRef> refs)
    {
        Map<InventoryRef, Inventory> locked = new LinkedHashMap<>();
        for (InventoryRef ref : new TreeSet<>(refs))
        {
            Inventory inventory = inventoryRepository
                    .findByProductIdAndLocationIdForUpdate(ref.productId(), ref.locationId())
                    .orElseThrow(() -> new IllegalStateException(
                            String.format("No %s inventory for product '%s' at location '%s'",
                                    ref.role(), ref.productSku(), ref.locationCode())));
            locked.put(ref, inventory);
        }
        return locked;
    }

    private Set<InventoryRef> sourceRefs(TransferOrder order)
    {
        Set<InventoryRef> refs = new LinkedHashSet<>();
        for (TransferOrderItem item : order.getItems())
        {
            refs.add(sourceRef(item, order.getSourceLocation()));
        }
        return refs;
    }

    private Set<InventoryRef> moveRefs(TransferOrder order)
    {
        Set<InventoryRef> refs = new LinkedHashSet<>();
        for (TransferOrderItem item : order.getItems())
        {
            refs.add(sourceRef(item, order.getSourceLocation()));
            refs.add(targetRef(item, order.getTargetLocation()));
        }
        return refs;
    }

    private InventoryRef sourceRef(TransferOrderItem item, Location location)
    {
        return refOf(item.getProduct(), location, "source");
    }

    private InventoryRef targetRef(TransferOrderItem item, Location location)
    {
        return refOf(item.getProduct(), location, "target");
    }

    private InventoryRef refOf(Product product, Location location, String role)
    {
        return new InventoryRef(product.getId(), location.getId(), product.getSku(), location.getCode(), role);
    }

    /**
     * Loads a transfer order with its items and locations eagerly to avoid
     * lazy-loading issues, or throws when it does not exist.
     *
     * @param id the order identifier
     * @return the transfer order entity
     * @throws TransferOrderNotFoundException if no order exists with the given id
     */
    private TransferOrder findWithDetails(UUID id)
    {
        return transferOrderRepository.findWithDetailsById(id)
                .orElseThrow(() -> new TransferOrderNotFoundException("id", id));
    }

    /**
     * Points at a single inventory record, carrying the display values needed to
     * build a meaningful error message when the record is missing.
     *
     * <p>Ordered by location first and product second, purely to give every
     * caller a single global lock order; the display values take no part in the
     * comparison.</p>
     *
     * @param productId    the product identifier
     * @param locationId   the location identifier
     * @param productSku   the product SKU, used in error messages
     * @param locationCode the location code, used in error messages
     * @param role         whether this is the source or the target side, used in error messages
     */
    private record InventoryRef(UUID productId,
                                UUID locationId,
                                String productSku,
                                String locationCode,
                                String role) implements Comparable<InventoryRef>
    {
        @Override
        public int compareTo(InventoryRef other)
        {
            int byLocation = this.locationId.compareTo(other.locationId);
            return byLocation != 0 ? byLocation : this.productId.compareTo(other.productId);
        }
    }
}
