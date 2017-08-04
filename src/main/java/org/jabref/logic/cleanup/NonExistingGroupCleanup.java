package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.jabref.Globals;
import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.groups.GroupTreeNode;

/**
 * Removes groups from entities that doesn't existing in database
 */
public class NonExistingGroupCleanup implements CleanupJob {

	private List<String> allGroupsList = new ArrayList<String>();

	@Override
	public List<FieldChange> cleanup(BibEntry entry) {
		List<FieldChange> changes = new ArrayList<>();

		//System.out.println("*-*-*");
		//System.out.println("article " + entry.getCiteKeyOptional().get());
		String oldFieldGropus = entry.getField(FieldName.GROUPS).get();
		String fieldGropus = "";
		List<String> allEntryGroups = new ArrayList<String>(Arrays.asList(oldFieldGropus.split("\\s*,\\s*")));
		boolean changed = false;


		if (allEntryGroups.size() == 0) {
			return changes;
		}

		Optional<GroupTreeNode> groupsFromDatabase = Globals.stateManager.getActiveDatabase().get().getMetaData().getGroups();
		readSubGroups(groupsFromDatabase);

		for (int i = 0; i < allEntryGroups.size(); i++){
//			System.out.println(" + " + allEntryGroups.get(i).trim());
			if (!allGroupsList.contains(allEntryGroups.get(i).trim())) {
//				System.out.println("      Brise se");
				allEntryGroups.remove(i--);
				changed = true;
			}
		}


		if (changed) {
			fieldGropus = String.join(", ", allEntryGroups); 
			System.out.println(oldFieldGropus);
			System.out.println(fieldGropus);
			
			entry.setField(FieldName.GROUPS, fieldGropus);

			FieldChange change = new FieldChange(entry, FieldName.GROUPS, oldFieldGropus, fieldGropus);
			changes.add(change);
		}

		return changes;
	}

	private void readSubGroups(Optional<GroupTreeNode> group){
		for (int i = 0; i < group.get().getNumberOfChildren(); i++) {
			allGroupsList.add(group.get().getChildAt(i).get().getName());
			readSubGroups(group.get().getChildAt(i));
		}
	}
}
