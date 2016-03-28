$(function() {
	
	// The sort icon is hidden to start with so that it's not shown unless this 
	// script is present.
	$('.no-show-sort-icon').removeClass('no-show-sort-icon');
	
	$('.sortable-tiles, .sort-table .sort-table-data-row').sortable({
		handle: '.sort-handle',
		stop: function(event, ui) {
			var sortedOrder = [];
			$(event.target).find('.table-tile').each(function(index) {
				var id = $(this).attr('data-id');
				if(id) {
					sortedOrder.push(id);
				}
			});
			var postLocation = $(event.target).find('.sort-form').attr('action');
			$.post(postLocation, {
				order: sortedOrder
			});
		}
	});
});