/*******************************************************************************
	 *
	 *	Copyright (C) 2008 Fujitsu Services Ltd.
	 *
	 *	Author: Nick Battle
	 *
	 *	This file is part of VDMJ.
	 *
	 *	VDMJ is free software: you can redistribute it and/or modify
	 *	it under the terms of the GNU General Public License as published by
	 *	the Free Software Foundation, either version 3 of the License, or
	 *	(at your option) any later version.
	 *
	 *	VDMJ is distributed in the hope that it will be useful,
	 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
	 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	 *	GNU General Public License for more details.
	 *
	 *	You should have received a copy of the GNU General Public License
	 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
	 *
	 ******************************************************************************
	 *REUSING FOR NOW
	 ******************************************************************************/
package eu.compassresearch.core.analysis.pog.obligations;

import org.overture.ast.lex.LexLocation;

/**
 * Core stuff needed
 */


abstract public class CMLProofObligation  implements Comparable<CMLProofObligation>
{

		public final LexLocation location;
		public final CMLPOType kind;
		public final String name;

		public int number;
		public String value;
		public POStatus status;
		//public POTrivialProof proof;

		private int var = 1;

		public CMLProofObligation(LexLocation location, CMLPOType kind,
				CMLPOContextStack ctxt)
		{
			this.location = location;
			this.kind = kind;
			this.name = ctxt.getName();
			this.status = POStatus.UNPROVED;
			//this.proof = null;
			this.number = 0;
		}

		public String getValue()
		{
			return value;
		}

		@Override
		public String toString()
		{
			return name + ": " + kind + " obligation " + location + "\n" + value;
		}

		protected String getVar(String root)
		{
			return root + var++;
		}

		//public void trivialCheck()
		//{
		//	for (POTrivialProof p : POTrivialProof.values())
		//	{
		//		if (p.proves(value))
		//		{
		//			status = POStatus.TRIVIAL;
		//			proof = p;
		//			break;
		//		}
		//	}
		//}

		public int compareTo(CMLProofObligation other)
		{
			return number - other.number;
		}
	}
